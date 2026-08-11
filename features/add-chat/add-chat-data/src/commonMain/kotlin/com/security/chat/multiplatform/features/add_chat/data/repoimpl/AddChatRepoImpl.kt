package com.security.chat.multiplatform.features.add_chat.data.repoimpl

import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.features.add_chat.data.entity.CreateChatRequest
import com.security.chat.multiplatform.features.add_chat.data.entity.CreateChatResponse
import com.security.chat.multiplatform.features.add_chat.data.entity.CreateGroupChatRequest
import com.security.chat.multiplatform.features.add_chat.data.entity.CreateGroupChatResponse
import com.security.chat.multiplatform.features.add_chat.data.entity.FindUserResponse
import com.security.chat.multiplatform.features.add_chat.data.entity.UserChatsResponse
import com.security.chat.multiplatform.features.add_chat.domain.entity.CreateChatResult
import com.security.chat.multiplatform.features.add_chat.domain.entity.FindUserResult
import com.security.chat.multiplatform.features.add_chat.domain.repo.AddChatRepo
import com.security.chat.multiplatform.features.chats.data.storage.ChatsStorage
import com.security.chat.multiplatform.features.chats.data.storage.entity.ChatSM
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import com.security.chat.multiplatform.features.users.data.network.UsersNetworkManager
import com.security.chat.multiplatform.features.users.data.network.entity.UserNM
import com.security.chat.multiplatform.features.users.data.storage.UsersStorage
import com.security.chat.multiplatform.features.users.data.storage.entity.UserSM

internal class AddChatRepoImpl(
    private val networkManagerFactory: NetworkManagerFactory,
    private val networkConfig: NetworkConfig,
    private val userStorage: UserStorage,
    private val chatsStorage: ChatsStorage,
    private val usersStorage: UsersStorage,
    private val usersNetworkManager: UsersNetworkManager,
) : AddChatRepo {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(
            baseUrl = "${networkConfig.host}:${networkConfig.port}",
            needAuthorization = true,
        )
    }

    override suspend fun findUser(username: String): FindUserResult {
        val response: FindUserResponse = networkManager.runGet(
            relativePath = "/users/find",
            request = mapOf(
                "login" to username,
            ),
        )

        return FindUserResult(
            userId = response.userId,
            login = response.login,
        )
    }

    override suspend fun createPersonalChat(secondUserId: String): CreateChatResult.PersonalChatCreated {
        val firstUserId = userStorage.getUserId() ?: error("user id not found")

        val result: CreateChatResponse = networkManager.runPost(
            relativePath = "/chats",
            request = CreateChatRequest(
                firstUserId = firstUserId,
                secondUserId = secondUserId,
            ),
        )

        return CreateChatResult.PersonalChatCreated(
            id = result.chatId,
        )
    }

    override suspend fun getUserId(): String {
        return checkNotNull(userStorage.getUserId())
    }

    override suspend fun createGroupChat(members: List<String>): CreateChatResult.GroupChatCreated {
        val response: CreateGroupChatResponse = networkManager.runPost(
            relativePath = "/group-chats",
            request = CreateGroupChatRequest(
                participantIds = members,
            ),
        )

        chatsStorage.saveChat(
            ChatSM.GroupChat(
                id = response.chatId,
                authorId = response.authorId,
                members = response.participantIds,
            ),
        )

        return CreateChatResult.GroupChatCreated(
            id = response.chatId,
        )
    }

    override suspend fun refreshChatsList() {
        val response: UserChatsResponse = networkManager.runGet(
            relativePath = "/chats",
        )

        val groupChats = response.groupChats
            .map { chatResponse ->
                // Ensure members and author are in UsersStorage
                (chatResponse.participantIds + chatResponse.authorId).distinct()
                    .forEach { memberId ->
                        if (usersStorage.getUser(memberId) == null) {
                            getAndSaveUser(memberId)
                        }
                    }

                ChatSM.GroupChat(
                    id = chatResponse.id,
                    authorId = chatResponse.authorId,
                    members = chatResponse.participantIds,
                )
            }

        chatsStorage.saveChats(chats = groupChats)
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
