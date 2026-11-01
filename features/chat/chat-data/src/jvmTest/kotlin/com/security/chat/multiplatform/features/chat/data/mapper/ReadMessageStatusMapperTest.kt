package com.security.chat.multiplatform.features.chat.data.mapper

import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageRecipientSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.Status
import com.security.chat.multiplatform.features.chat.domain.entity.MessageAuthor
import com.security.chat.multiplatform.features.chat.domain.entity.MessageStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadMessageStatusMapperTest {
    @Test
    fun `group outgoing message becomes read only after every other recipient reads it`() {
        val message = MessageSM.Text(
            id = "message",
            chatId = "chat",
            recipients = emptyList(),
            authorId = "me",
            timestamp = 1,
            text = "text",
        )
        for (status in Status.entries) {
            val mapped = message.copy(
                recipients = listOf(
                    MessageRecipientSM("me", Status.Sent),
                    MessageRecipientSM("peer", Status.Read),
                    MessageRecipientSM("other", status),
                ),
            ).toDomain(
                appOwnerId = "me",
                author = MessageAuthor(id = "me", name = "Me"),
                imagesDirectoryPath = "/images",
            )
            assertEquals(
                when (status) {
                    Status.Created -> MessageStatus.Created
                    Status.Sent -> MessageStatus.Sent
                    Status.ReceivedByReceiver -> MessageStatus.ReceivedByReceiver
                    Status.Read -> MessageStatus.Read
                },
                mapped.status,
            )
        }
    }
}
