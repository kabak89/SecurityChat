package com.security.chat.multiplatform.features.chat.data.network

import com.security.chat.multiplatform.common.core.network.LiveEventsManager
import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.features.chat.data.network.entity.ChatMessageNM
import com.security.chat.multiplatform.features.chat.data.network.entity.network.ChatMessage
import com.security.chat.multiplatform.features.chat.data.network.entity.network.ChatSubscribeMessage
import com.security.chat.multiplatform.features.chat.data.network.entity.network.GetMessagesResponse
import com.security.chat.multiplatform.features.chat.data.network.entity.network.MessagesReceivedRequest
import com.security.chat.multiplatform.features.chat.data.network.mapper.toNM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.Json

public interface ChatNetworkManager {
    public suspend fun getMessages(chatId: String): List<ChatMessageNM>
    public suspend fun getNewMessagesFlow(chatId: String, authorId: String): Flow<ChatMessageNM>
    public suspend fun processNewMessages(serializedMessages: String): List<ChatMessageNM>
    public suspend fun confirmReceivingMessages(
        chatId: String,
        messageIds: List<String>,
    )

    public suspend fun downloadFile(fileId: String, destinationPath: String)
}

internal class ChatNetworkManagerImpl(
    private val networkManagerFactory: NetworkManagerFactory,
    private val networkConfig: NetworkConfig,
    private val liveEventsManager: LiveEventsManager,
    private val json: Json,
) : ChatNetworkManager {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(
            baseUrl = "${networkConfig.host}:${networkConfig.port}",
            needAuthorization = true,
        )
    }

    override suspend fun getMessages(chatId: String): List<ChatMessageNM> {
        return networkManager.runGet<GetMessagesResponse>(
            relativePath = "/messages",
            request = mapOf("chat-id" to chatId),
        )
            .messages
            .mapNotNull { it.toNM(json = json) }
    }

    override suspend fun getNewMessagesFlow(
        chatId: String,
        authorId: String,
    ): Flow<ChatMessageNM> {
        val subscribeMessage = ChatSubscribeMessage(
            chatId = chatId,
            authorId = authorId,
        )

        return liveEventsManager
            .subscribe<ChatMessage, ChatSubscribeMessage>(
                subscribeMessage = subscribeMessage,
                type = "chat_message",
            )
            .mapNotNull { it.toNM(json = json) }
    }

    override suspend fun processNewMessages(serializedMessages: String): List<ChatMessageNM> {
        return json.decodeFromString<GetMessagesResponse>(serializedMessages).messages
            .mapNotNull { it.toNM(json = json) }
    }

    override suspend fun confirmReceivingMessages(
        chatId: String,
        messageIds: List<String>,
    ) {
        networkManager.runPost<MessagesReceivedRequest, Unit>(
            relativePath = "/messages/received",
            request = MessagesReceivedRequest(
                chatId = chatId,
                messageIds = messageIds,
            ),
        )
    }

    override suspend fun downloadFile(fileId: String, destinationPath: String) {
        networkManager.runGetFile(
            relativePath = "/files/$fileId",
            destinationPath = destinationPath,
        )
    }
}