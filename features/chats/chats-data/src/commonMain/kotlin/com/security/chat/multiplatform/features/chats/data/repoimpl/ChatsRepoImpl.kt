package com.security.chat.multiplatform.features.chats.data.repoimpl

import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.features.chats.data.entity.CreateChatRequest
import com.security.chat.multiplatform.features.chats.data.entity.CreateChatResponse
import com.security.chat.multiplatform.features.chats.data.entity.FindUserResponse
import com.security.chat.multiplatform.features.chats.domain.entity.CreateChatResult
import com.security.chat.multiplatform.features.chats.domain.entity.FindUserResult
import com.security.chat.multiplatform.features.chats.domain.repo.ChatsRepo
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode

internal class ChatsRepoImpl(
    private val networkManagerFactory: NetworkManagerFactory,
    private val userStorage: UserStorage,
) : ChatsRepo {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(baseUrl = "http://13.60.146.92:80")
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
}