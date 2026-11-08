package com.security.chat.multiplatform.features.chat.data.network.mapper

import com.security.chat.multiplatform.common.core.network.di.coreNetworkModule
import com.security.chat.multiplatform.features.chat.data.network.entity.ChatMessageNM
import com.security.chat.multiplatform.features.chat.data.network.entity.network.ChatMessage
import kotlinx.serialization.json.Json
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNull

class ChatDataNetworkMapperTest {

    @Test
    fun `messages without a reply field are accepted from older servers`() {
        val app = koinApplication { modules(coreNetworkModule) }
        try {
            val json = app.koin.get<Json>()
            val text = ChatMessage(
                type = "text",
                message = """
                    {"id":"text","key":"key","authorId":"author","timestamp":1,"text":"ciphertext"}
                """.trimIndent(),
            )
            val image = ChatMessage(
                type = "image",
                message = """
                    {"id":"image","key":"key","authorId":"author","timestamp":2,"fileId":"file"}
                """.trimIndent(),
            )

            assertNull(assertIs<ChatMessageNM.Text>(text.toNM(json)).replyToMessageId)
            assertNull(assertIs<ChatMessageNM.Image>(image.toNM(json)).replyToMessageId)
        } finally {
            app.close()
        }
    }
}
