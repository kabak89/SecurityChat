package com.security.chat.multiplatform.features.chat.data.storage.mapper

import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.chat.data.storage.ImageMessageTable
import com.security.chat.multiplatform.features.chat.data.storage.MessageTable
import com.security.chat.multiplatform.features.chat.data.storage.TextMessageTable
import com.security.chat.multiplatform.features.chat.data.storage.entity.JoinedMessageRow
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.Status

internal fun MessageSM.toMessageTable(): MessageTable {
    return MessageTable(
        id = id,
        chatId = chatId,
        authorId = authorId,
        status = mapStatusToString(status = status),
        timestamp = timestamp,
        type = mapTypeToString(message = this),
    )
}

internal fun MessageSM.Text.toTextTable(): TextMessageTable {
    return TextMessageTable(
        id = id,
        text = text,
    )
}

internal fun MessageSM.Image.toImageTable(): ImageMessageTable {
    return ImageMessageTable(
        id = id,
        fileId = fileId,
        key = key,
        localPath = localPath,
    )
}

internal fun JoinedMessageRow.toSM(recipients: List<String>): MessageSM? {
    val status = mapStringToStatus(status) ?: return null
    return when (type) {
        TYPE_TEXT -> MessageSM.Text(
            id = id,
            chatId = chatId,
            text = text ?: run {
                Log.e("missing text detail for message id=$id")
                return null
            },
            authorId = authorId,
            status = status,
            timestamp = timestamp,
            recipients = recipients,
        )

        TYPE_IMAGE -> MessageSM.Image(
            id = id,
            chatId = chatId,
            fileId = fileId ?: run {
                Log.e("missing image detail for message id=$id")
                return null
            },
            key = key ?: run {
                Log.e("missing image key for message id=$id")
                return null
            },
            localPath = localPath,
            authorId = authorId,
            status = status,
            timestamp = timestamp,
            recipients = recipients,
        )

        else -> {
            Log.e("unknown type: $type")
            null
        }
    }
}

private fun mapTypeToString(message: MessageSM): String {
    return when (message) {
        is MessageSM.Text -> TYPE_TEXT
        is MessageSM.Image -> TYPE_IMAGE
    }
}

private fun mapStatusToString(status: Status): String {
    return when (status) {
        Status.Created -> "Created"
        Status.Sent -> "Sent"
        Status.Received -> "Received"
    }
}

private fun mapStringToStatus(string: String): Status? {
    Status.entries.forEach { status ->
        if (mapStatusToString(status) == string) {
            return status
        }
    }

    Log.e("unknown status: $string")
    return null
}

private const val TYPE_TEXT: String = "Text"
private const val TYPE_IMAGE: String = "Image"
