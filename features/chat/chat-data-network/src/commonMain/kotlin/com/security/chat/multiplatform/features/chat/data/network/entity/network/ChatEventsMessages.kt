package com.security.chat.multiplatform.features.chat.data.network.entity.network

import kotlinx.serialization.Serializable

@Serializable
internal data class ChatEventsSubscribeMessage(
    val scope: String,
    val chatId: String,
)

@Serializable
internal data class ChatEventEnvelope(
    val id: String? = null,
    val type: String,
    val payload: String,
)

@Serializable
internal data class MessageReceivedEventPayload(
    val messageId: String,
    val chatId: String,
    val receivedByUserId: String,
)

@Serializable
internal data class MessageReadEventPayload(
    val messageId: String,
    val chatId: String,
    val readByUserId: String,
)

@Serializable
internal data class MessageDeleteRequest(val chatId: String, val messageId: String)

@Serializable
internal data class MessageDeletedEventPayload(val messageId: String, val deletedByUserId: String)

@Serializable
internal data class ChatEventAcknowledgementRequest(
    val eventId: String,
    val eventType: String,
)

@Serializable
internal data class ChatEventPublishRequest(
    val recipientUserId: String,
    val chatId: String,
    val event: ChatEventEnvelope,
)

@Serializable
internal data class ChatEventAcknowledgement(val eventId: String)

@Serializable
internal data class ChatEventError(val code: String, val message: String)

internal const val MESSAGE_RECEIVED_EVENT_TYPE: String = "message.received"
internal const val MESSAGE_READ_EVENT_TYPE: String = "message.read"
internal const val MESSAGE_DELETED_EVENT_TYPE: String = "message.deleted"
