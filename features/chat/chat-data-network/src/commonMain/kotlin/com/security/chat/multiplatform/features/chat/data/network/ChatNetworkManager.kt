package com.security.chat.multiplatform.features.chat.data.network

import com.security.chat.multiplatform.common.core.network.LiveEventsManager
import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.common.core.network.entity.SocketEvent
import com.security.chat.multiplatform.features.chat.data.network.entity.ChatEventNM
import com.security.chat.multiplatform.features.chat.data.network.entity.ChatMessageNM
import com.security.chat.multiplatform.features.chat.data.network.entity.network.ChatEventAcknowledgement
import com.security.chat.multiplatform.features.chat.data.network.entity.network.ChatEventAcknowledgementRequest
import com.security.chat.multiplatform.features.chat.data.network.entity.network.ChatEventEnvelope
import com.security.chat.multiplatform.features.chat.data.network.entity.network.ChatEventError
import com.security.chat.multiplatform.features.chat.data.network.entity.network.ChatEventPublishRequest
import com.security.chat.multiplatform.features.chat.data.network.entity.network.ChatEventsSubscribeMessage
import com.security.chat.multiplatform.features.chat.data.network.entity.network.ChatMessage
import com.security.chat.multiplatform.features.chat.data.network.entity.network.ChatSubscribeMessage
import com.security.chat.multiplatform.features.chat.data.network.entity.network.GetMessagesResponse
import com.security.chat.multiplatform.features.chat.data.network.entity.network.MESSAGE_RECEIVED_EVENT_TYPE
import com.security.chat.multiplatform.features.chat.data.network.entity.network.MessageReceivedEventPayload
import com.security.chat.multiplatform.features.chat.data.network.entity.network.MessagesReceivedRequest
import com.security.chat.multiplatform.features.chat.data.network.mapper.toChatEventNM
import com.security.chat.multiplatform.features.chat.data.network.mapper.toNM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.time.Duration.Companion.seconds

public interface ChatNetworkManager {
    public suspend fun getMessages(chatId: String): List<ChatMessageNM>

    public suspend fun getNewMessagesFlow(
        chatId: String,
        authorId: String,
        deviceId: String,
    ): Flow<ChatMessageNM>

    public suspend fun processNewMessages(serializedMessages: String): List<ChatMessageNM>

    public fun getEventsFlow(chatId: String): Flow<ChatEventNM>
    public suspend fun acknowledgeEvent(event: ChatEventNM)
    public suspend fun publishMessageReceivedEvent(
        eventId: String,
        messageId: String,
        chatId: String,
        recipientUserId: String,
        receivedByUserId: String,
    )

    public suspend fun confirmReceivingMessages(
        chatId: String,
        deviceId: String,
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
        deviceId: String,
    ): Flow<ChatMessageNM> {
        val subscribeMessage = ChatSubscribeMessage(
            chatId = chatId,
            authorId = authorId,
            deviceId = deviceId,
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

    override fun getEventsFlow(chatId: String): Flow<ChatEventNM> {
        return liveEventsManager
            .subscribeEvents<JsonElement, ChatEventsSubscribeMessage>(
                subscribeMessage = ChatEventsSubscribeMessage(scope = "chat", chatId = chatId),
                type = "events",
            )
            .mapNotNull { event ->
                when (event) {
                    SocketEvent.Disconnected -> null
                    is SocketEvent.Message -> event.value.toChatEventNM(
                        connectionId = event.connectionId,
                        json = json,
                    )
                }
            }
    }

    override suspend fun acknowledgeEvent(event: ChatEventNM) {
        val type = when (event) {
            is ChatEventNM.MessageReceived -> MESSAGE_RECEIVED_EVENT_TYPE
        }
        liveEventsManager.send(
            message = ChatEventAcknowledgementRequest(eventId = event.id, eventType = type),
            type = "event.ack",
            connectionId = event.connectionId,
        )
    }

    override suspend fun confirmReceivingMessages(
        chatId: String,
        deviceId: String,
        messageIds: List<String>,
    ) {
        networkManager.runPost<MessagesReceivedRequest, Unit>(
            relativePath = "/messages/received",
            request = MessagesReceivedRequest(
                chatId = chatId,
                messageIds = messageIds,
                deviceId = deviceId,
            ),
        )
    }

    override suspend fun publishMessageReceivedEvent(
        eventId: String,
        messageId: String,
        chatId: String,
        recipientUserId: String,
        receivedByUserId: String,
    ) {
        val request = ChatEventPublishRequest(
            recipientUserId = recipientUserId,
            chatId = chatId,
            event = ChatEventEnvelope(
                id = eventId,
                type = MESSAGE_RECEIVED_EVENT_TYPE,
                payload = json.encodeToString(
                    MessageReceivedEventPayload(
                        messageId = messageId,
                        chatId = chatId,
                        receivedByUserId = receivedByUserId,
                    ),
                ),
            ),
        )
        val acknowledgement = withTimeoutOrNull(15.seconds) {
            liveEventsManager.subscribe<ChatEventEnvelope, ChatEventPublishRequest>(
                subscribeMessage = request,
                type = "event.publish",
            )
                .onEach { response ->
                    if (response.type == "events.error") {
                        val eventError = json.decodeFromString<ChatEventError>(response.payload)
                        error("Event publication failed: ${eventError.code}: ${eventError.message}")
                    }
                }
                .first { response ->
                    response.type == "events.published" &&
                            json.decodeFromString<ChatEventAcknowledgement>(response.payload)
                                .eventId == eventId
                }
        }
        checkNotNull(acknowledgement) { "Timed out publishing delivery event: id=$eventId" }
    }

    override suspend fun downloadFile(fileId: String, destinationPath: String) {
        networkManager.runGetFile(
            relativePath = "/files/$fileId",
            destinationPath = destinationPath,
        )
    }
}
