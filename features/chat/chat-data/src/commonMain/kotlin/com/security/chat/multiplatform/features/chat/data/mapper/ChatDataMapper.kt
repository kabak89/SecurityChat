package com.security.chat.multiplatform.features.chat.data.mapper

import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.Status
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.entity.MessageAuthor
import com.security.chat.multiplatform.features.chat.domain.entity.MessageDirection
import com.security.chat.multiplatform.features.chat.domain.entity.MessageRecipient
import com.security.chat.multiplatform.features.chat.domain.entity.MessageStatus

internal fun MessageSM.toDomain(
    appOwnerId: String,
    author: MessageAuthor,
    imagesDirectoryPath: String,
): Message {
    val direction = if (authorId == appOwnerId) {
        MessageDirection.Outgoing
    } else {
        MessageDirection.Incoming
    }

    val status = when (direction) {
        MessageDirection.Incoming -> recipients.firstOrNull { it.userId == appOwnerId }?.status
            ?: Status.ReceivedByReceiver

        MessageDirection.Outgoing -> recipients
            .filter { it.userId != authorId }
            .minByOrNull { it.status.ordinal }
            ?.status
            ?: if (recipients.any { it.status == Status.Created }) Status.Created else Status.Sent
    }
    val domainRecipients = recipients.map {
        MessageRecipient(userId = it.userId, status = it.status.toDomain())
    }

    return when (this) {
        is MessageSM.Text -> Message.Text(
            id = id,
            author = author,
            timestamp = timestamp,
            direction = direction,
            recipients = domainRecipients,
            status = status.toDomain(),
            text = text,
        )

        is MessageSM.Image -> Message.Image(
            id = id,
            author = author,
            timestamp = timestamp,
            direction = direction,
            recipients = domainRecipients,
            status = status.toDomain(),
            filePath = if (isDownloaded) "$imagesDirectoryPath/$fileId" else null,
        )
    }
}

private fun Status.toDomain(): MessageStatus {
    return when (this) {
        Status.Created -> MessageStatus.Created
        Status.Sent -> MessageStatus.Sent
        Status.ReceivedByReceiver -> MessageStatus.ReceivedByReceiver
        Status.Read -> MessageStatus.Read
    }
}
