package com.security.chat.multiplatform.features.chat.data.network.mapper

import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.chat.data.network.entity.ChatEventNM
import com.security.chat.multiplatform.features.chat.data.network.entity.network.ChatEventEnvelope
import com.security.chat.multiplatform.features.chat.data.network.entity.network.MESSAGE_READ_EVENT_TYPE
import com.security.chat.multiplatform.features.chat.data.network.entity.network.MESSAGE_RECEIVED_EVENT_TYPE
import com.security.chat.multiplatform.features.chat.data.network.entity.network.MessageReadEventPayload
import com.security.chat.multiplatform.features.chat.data.network.entity.network.MessageReceivedEventPayload
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

internal fun JsonElement.toChatEventNM(connectionId: Long, json: Json): ChatEventNM? {
    return try {
        val envelope = json.decodeFromJsonElement<ChatEventEnvelope>(this)
        when (envelope.type) {
            MESSAGE_RECEIVED_EVENT_TYPE -> {
                val id = envelope.id?.takeIf { it.isNotBlank() }
                if (id == null) {
                    Log.e("Skipped message.received event without an id")
                    return null
                }
                val payload = json.decodeFromString<MessageReceivedEventPayload>(envelope.payload)
                ChatEventNM.MessageReceived(
                    id = id,
                    connectionId = connectionId,
                    messageId = payload.messageId,
                    chatId = payload.chatId,
                    receivedByUserId = payload.receivedByUserId,
                )
            }

            MESSAGE_READ_EVENT_TYPE -> {
                val id = envelope.id?.takeIf { it.isNotBlank() }
                if (id == null) {
                    Log.e("Skipped message.read event without an id")
                    return null
                }
                val payload = json.decodeFromString<MessageReadEventPayload>(envelope.payload)
                ChatEventNM.MessageRead(
                    id = id,
                    connectionId = connectionId,
                    messageId = payload.messageId,
                    chatId = payload.chatId,
                    readByUserId = payload.readByUserId,
                )
            }

            "events.error" -> {
                Log.e("Chat events subscription error: ${envelope.payload}")
                null
            }

            else -> {
                Log.e("Skipped unknown chat event type: ${envelope.type}, id=${envelope.id}")
                null
            }
        }
    } catch (error: SerializationException) {
        Log.e(error, "Skipped malformed chat event")
        null
    } catch (error: IllegalArgumentException) {
        Log.e(error, "Skipped invalid chat event")
        null
    }
}
