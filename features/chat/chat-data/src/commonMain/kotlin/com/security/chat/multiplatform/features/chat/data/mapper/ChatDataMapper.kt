package com.security.chat.multiplatform.features.chat.data.mapper

import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.Status
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.entity.MessageAuthor
import com.security.chat.multiplatform.features.chat.domain.entity.MessageDirection
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

    return when (this) {
        is MessageSM.Text -> Message.Text(
            id = id,
            author = author,
            timestamp = timestamp,
            direction = direction,
            status = status.toDomain(),
            text = text,
        )

        is MessageSM.Image -> Message.Image(
            id = id,
            author = author,
            timestamp = timestamp,
            direction = direction,
            status = status.toDomain(),
            filePath = if (isDownloaded) "$imagesDirectoryPath/$fileId" else null,
        )
    }
}

private fun Status.toDomain(): MessageStatus {
    return when (this) {
        Status.Created -> MessageStatus.Created
        Status.Sending -> MessageStatus.Sending
        Status.Sent -> MessageStatus.Sent
        Status.Received -> MessageStatus.Received
    }
}
