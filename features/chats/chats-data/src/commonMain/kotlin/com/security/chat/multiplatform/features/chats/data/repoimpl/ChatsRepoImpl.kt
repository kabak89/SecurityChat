package com.security.chat.multiplatform.features.chats.data.repoimpl

import com.security.chat.multiplatform.common.core.network.ConnectivityObserver
import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.features.chats.data.entity.CreateChatRequest
import com.security.chat.multiplatform.features.chats.data.entity.CreateChatResponse
import com.security.chat.multiplatform.features.chats.data.entity.FindUserResponse
import com.security.chat.multiplatform.features.chats.data.entity.UserChatsResponse
import com.security.chat.multiplatform.features.chats.data.mapper.toDomain
import com.security.chat.multiplatform.features.chats.data.mapper.toSM
import com.security.chat.multiplatform.features.chats.data.storage.ChatsStorage
import com.security.chat.multiplatform.features.chats.domain.entity.ChatDescription
import com.security.chat.multiplatform.features.chats.domain.entity.CreateChatResult
import com.security.chat.multiplatform.features.chats.domain.entity.FindUserResult
import com.security.chat.multiplatform.features.chats.domain.repo.ChatsRepo
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import com.security.chat.multiplatform.features.users.data.network.UsersNetworkManager
import com.security.chat.multiplatform.features.users.data.network.entity.UserNM
import com.security.chat.multiplatform.features.users.data.storage.UsersStorage
import com.security.chat.multiplatform.features.users.data.storage.entity.UserSM
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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
        networkManagerFactory.build(baseUrl = "${networkConfig.host}:${networkConfig.port}")
    }

    override suspend fun findUser(username: String): FindUserResult {
        return try {
            val response: FindUserResponse = networkManager.runGet(
                relativePath = "/users/find",
                request = mapOf(
                    "login" to username,
                ),
            )

            FindUserResult.UserFound(
                userId = response.userId,
                login = response.login,
                publicKey = response.publicKey,
            )
        } catch (e: ClientRequestException) {
            when (e.response.status) {
                HttpStatusCode.NotFound -> FindUserResult.NotFound
                else -> throw e
            }
        }
    }

    override suspend fun createChat(secondUserId: String): CreateChatResult {
        val firstUserId = userStorage.getUserId() ?: error("user id not found")

        val result: CreateChatResponse = networkManager.runPost(
            relativePath = "/chats",
            request = CreateChatRequest(
                firstUserId = firstUserId,
                secondUserId = secondUserId,
            ),
        )

        return CreateChatResult.ChatCreated(
            id = result.chatId,
        )
    }

    override fun getChatsListFlow(): Flow<List<ChatDescription>> {
        return chatsStorage.getChatsFlow()
            .map { chatList ->
                chatList.map { chatSM ->
                    val companionId = chatSM.companionId
                    val companionName = usersStorage.getUser(companionId)?.name ?: run {
                        val user = getAndSaveUser(companionId)
                        user.name
                    }
                    chatSM.toDomain(companionName = companionName)
                }
            }
            .distinctUntilChanged()
    }

    override suspend fun fetchChatsList() {
        val userId = userStorage.getUserId() ?: error("user id not found")

        val response: UserChatsResponse = networkManager.runGet(
            relativePath = "/chats",
            request = mapOf(
                "user_id" to userId,
            ),
        )

        val chats = response.chats
            .map { chatResponse ->
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

        val storageModels = chats.map { it.toSM() }
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