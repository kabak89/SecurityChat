package com.security.chat.multiplatform.features.chat.data.network.mapper

import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.chat.data.network.entity.ChatEventNM
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChatEventMapperTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val payload =
        """{"messageId":"message","chatId":"chat","receivedByUserId":"peer"}"""
    private val readPayload =
        """{"messageId":"message","chatId":"chat","readByUserId":"peer"}"""

    @Test
    fun `read message decodes the serialized payload and retains the transport session`() {
        val event = assertIs<ChatEventNM.MessageRead>(
            envelope(type = "message.read", payload = readPayload)
                .toChatEventNM(connectionId = 12L, json = json),
        )
        assertEquals("event", event.id)
        assertEquals(12L, event.connectionId)
        assertEquals("message", event.messageId)
        assertEquals("chat", event.chatId)
        assertEquals("peer", event.readByUserId)
    }

    @Test
    fun `malformed read payload and missing or blank event id are skipped`() {
        for (payload in listOf("{}", "not JSON", this.payload)) {
            assertNull(
                envelope(type = "message.read", payload = payload)
                    .toChatEventNM(connectionId = 1L, json = json),
            )
        }
        for (id in listOf(null, "", " ")) {
            assertNull(
                envelope(type = "message.read", payload = readPayload, id = id)
                    .toChatEventNM(connectionId = 1L, json = json),
            )
        }
    }

    @Test
    fun `received message decodes the serialized payload and retains the transport session`() {
        val event = assertIs<ChatEventNM.MessageReceived>(
            envelope(type = "message.received", payload = payload)
                .toChatEventNM(connectionId = 12L, json = json),
        )
        assertEquals("event", event.id)
        assertEquals(12L, event.connectionId)
        assertEquals("message", event.messageId)
        assertEquals("chat", event.chatId)
        assertEquals("peer", event.receivedByUserId)
    }

    @Test
    fun `unknown type logs and skips its payload without blocking a known event`() {
        val errors = mutableListOf<String>()
        Log.addSink { _, message -> message?.let { errors += it } }

        assertNull(
            envelope(type = "future.event", payload = "not JSON")
                .toChatEventNM(connectionId = 1L, json = json),
        )
        assertTrue(errors.any { it.contains("future.event") })
        assertIs<ChatEventNM.MessageReceived>(
            envelope(type = "message.received", payload = payload)
                .toChatEventNM(connectionId = 1L, json = json),
        )
    }

    @Test
    fun `malformed payload and missing event id are skipped`() {
        assertNull(
            envelope(type = "message.received", payload = "{}")
                .toChatEventNM(connectionId = 1L, json = json),
        )
        assertNull(
            envelope(type = "message.received", payload = payload, id = null)
                .toChatEventNM(connectionId = 1L, json = json),
        )
    }

    private fun envelope(type: String, payload: String, id: String? = "event"): JsonElement =
        buildJsonObject {
            if (id != null) put("id", id)
            put("type", type)
            put("payload", payload)
        }
}
