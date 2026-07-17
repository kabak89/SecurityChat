package com.security.chat.multiplatform.features.chat.data.storage.mapper

import com.security.chat.multiplatform.features.chat.data.storage.TextMessageTable
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.Status

internal fun MessageSM.toTable(): TextMessageTable {
    return TextMessageTable(
        id = id,
        chatId = chatId,
        text = when (this) {
            is MessageSM.Text -> text
        },
        authorId = authorId,
        status = mapStatusToString(status = status),
        timestamp = timestamp,
    )
}

internal fun TextMessageTable.toSM(
    recipients: List<String>,
): MessageSM? {
    return MessageSM.Text(
        id = id,
        chatId = chatId,
        text = text,
        authorId = authorId,
        status = mapStringToStatus(status) ?: return null,
        timestamp = timestamp,
        recipients = recipients,
    )
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

    return null
}
