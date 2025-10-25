package com.security.chat.multiplatform.features.chats.data.repoimpl

import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.features.chats.data.entity.CreateChatRequest
import com.security.chat.multiplatform.features.chats.data.entity.CreateChatResponse
import com.security.chat.multiplatform.features.chats.data.entity.FindUserResponse
import com.security.chat.multiplatform.features.chats.data.entity.UserChatsResponse
import com.security.chat.multiplatform.features.chats.data.mapper.toSM
import com.security.chat.multiplatform.features.chats.data.storage.ChatsStorage
import com.security.chat.multiplatform.features.chats.domain.entity.ChatDescription
import com.security.chat.multiplatform.features.chats.domain.entity.CreateChatResult
import com.security.chat.multiplatform.features.chats.domain.entity.FindUserResult
import com.security.chat.multiplatform.features.chats.domain.repo.ChatsRepo
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode

internal class ChatsRepoImpl(
    private val networkManagerFactory: NetworkManagerFactory,
    private val userStorage: UserStorage,
    private val chatsStorage: ChatsStorage,
) : ChatsRepo {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(baseUrl = "http://192.168.1.5:80")
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

    override suspend fun getChatsList(): List<ChatDescription> {
        val userId = userStorage.getUserId() ?: error("user id not found")

        val response: UserChatsResponse = networkManager.runGet(
            relativePath = "/chats",
            request = mapOf(
                "user_id" to userId,
            ),
        )

        val chats = response.chats
            .map { chatResponse ->
                ChatDescription(
                    id = chatResponse.id,
                    firstUserId = chatResponse.firstUserId,
                    secondUserId = chatResponse.secondUserId,
                )
            }

        val storageModels = chats.map { it.toSM() }
        chatsStorage.saveChats(chats = storageModels)

        return chats
    }
}