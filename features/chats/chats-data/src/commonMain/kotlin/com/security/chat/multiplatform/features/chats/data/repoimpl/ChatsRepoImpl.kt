package com.security.chat.multiplatform.features.chats.data.repoimpl

import com.security.chat.multiplatform.common.core.network.ConnectivityObserver
import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.features.chats.data.entity.UserChatsResponse
import com.security.chat.multiplatform.features.chats.data.mapper.toDomain
import com.security.chat.multiplatform.features.chats.data.mapper.toSM
import com.security.chat.multiplatform.features.chats.data.storage.ChatsStorage
import com.security.chat.multiplatform.features.chats.data.storage.entity.ChatSM
import com.security.chat.multiplatform.features.chats.domain.entity.Chat
import com.security.chat.multiplatform.features.chats.domain.entity.ChatMember
import com.security.chat.multiplatform.features.chats.domain.repo.ChatsRepo
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import com.security.chat.multiplatform.features.users.data.network.UsersNetworkManager
import com.security.chat.multiplatform.features.users.data.network.entity.UserNM
import com.security.chat.multiplatform.features.users.data.storage.UsersStorage
import com.security.chat.multiplatform.features.users.data.storage.entity.UserSM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest

internal class ChatsRepoImpl(
    private val networkManagerFactory: NetworkManagerFactory,
    private val userStorage: UserStorage,
    private val chatsStorage: ChatsStorage,
    private val usersStorage: UsersStorage,
    private val usersNetworkManager: UsersNetworkManager,
    private val connectivityObserver: ConnectivityObserver,
    private val networkConfig: NetworkConfig,
) : ChatsRepo {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(
            baseUrl = "${networkConfig.host}:${networkConfig.port}",
            needAuthorization = true,
        )
    }

    override fun getChatsListFlow(): Flow<List<Chat>> {
        return chatsStorage.getChatsFlow()
            .flatMapLatest { chatList ->
                if (chatList.isEmpty()) return@flatMapLatest kotlinx.coroutines.flow.flowOf(emptyList())

                val userFlows = chatList
                    .flatMap { chat ->
                        when (chat) {
                            is ChatSM.PersonalChat -> {
                                listOf(usersStorage.getUserFlow(chat.interlocutorId))
                            }

                            is ChatSM.GroupChat -> {
                                val members = chat.members + chat.authorId
                                members.map { memberId ->
                                    usersStorage.getUserFlow(memberId)
                                }
                            }
                        }
                    }

                combine(userFlows) { users ->
                    chatList
                        .map { chatSM ->
                            when (chatSM) {
                                is ChatSM.GroupChat -> {
                                    val members = chatSM.members
                                        .map { userId ->
                                            users.find { it?.id == userId } ?: run {
                                                val userNM = getAndSaveUser(userId)
                                                UserSM(
                                                    id = userNM.userId,
                                                    publicKey = userNM.publicKey,
                                                    name = userNM.name,
                                                )
                                            }
                                        }
                                        .map { it ->
                                            ChatMember(
                                                id = it.id,
                                                username = it.name,
                                            )
                                        }

                                    val author = chatSM.authorId.let { authorId ->
                                        users.find { it?.id == authorId } ?: run {
                                            val userNM = getAndSaveUser(authorId)
                                            UserSM(
                                                id = userNM.userId,
                                                publicKey = userNM.publicKey,
                                                name = userNM.name,
                                            )
                                        }
                                    }

                                    chatSM.toDomain(
                                        members = members,
                                        author = ChatMember(
                                            id = chatSM.authorId,
                                            username = author.name,
                                        ),
                                    )
                                }

                                is ChatSM.PersonalChat -> {
                                    val interlocutorId = chatSM.interlocutorId
                                    val interlocutorName =
                                        users.find { it?.id == interlocutorId }?.name ?: run {
                                            val user = getAndSaveUser(interlocutorId)
                                            user.name
                                        }
                                    chatSM.toDomain(interlocutorName = interlocutorName)
                                }
                            }
                        }
                }
            }
            .distinctUntilChanged()
    }

    override suspend fun fetchChatsList() {
        val response: UserChatsResponse = networkManager.runGet(
            relativePath = "/chats",
        )

        val personalChats = response.personalChats
            .map { chatResponse ->
                val userId = userStorage.getUserId() ?: error("user id not found")

                val companionId = if (chatResponse.firstUserId == userId) {
                    chatResponse.secondUserId
                } else {
                    chatResponse.firstUserId
                }

                val companionName = usersStorage.getUser(companionId)?.name ?: run {
                    val user = getAndSaveUser(companionId)
                    user.name
                }

                chatResponse.toDomain(
                    companionName = companionName,
                    companionId = companionId,
                )
            }

        val groupChats = response.groupChats
            .map { chatResponse ->
                val members = chatResponse.participantIds
                    .map { memberId ->
                        usersStorage.getUser(memberId) ?: run {
                            val user = getAndSaveUser(memberId)
                            UserSM(
                                id = user.userId,
                                publicKey = user.publicKey,
                                name = user.name,
                            )
                        }
                    }
                    .map {
                        ChatMember(
                            id = it.id,
                            username = it.name,
                        )
                    }

                val userForAuthor = usersStorage.getUser(chatResponse.authorId) ?: run {
                    val user = getAndSaveUser(chatResponse.authorId)
                    UserSM(
                        id = user.userId,
                        publicKey = user.publicKey,
                        name = user.name,
                    )
                }

                val author = ChatMember(
                    id = chatResponse.authorId,
                    username = userForAuthor.name,
                )

                chatResponse.toDomain(
                    members = members,
                    author = author,
                )
            }

        val storageModels = personalChats.map { it.toSM() } + groupChats.map { it.toSM() }
        chatsStorage.saveChats(chats = storageModels)
    }

    override fun isConnectedToInternetFlow(): Flow<Boolean> {
        return connectivityObserver.isOnline
    }

    private suspend fun getAndSaveUser(id: String): UserNM {
        val user = usersNetworkManager.getUser(id)
        usersStorage.saveUser(
            user = UserSM(
                id = user.userId,
                publicKey = user.publicKey,
                name = user.name,
            ),
        )
        return user
    }
}
