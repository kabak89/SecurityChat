package com.security.chat.multiplatform.features.chat.data.storage.mapper

import com.security.chat.multiplatform.features.chat.data.storage.MessageTable
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM

internal fun MessageSM.toTable(): MessageTable {
    return MessageTable(
        id = id,
        chatId = chatId,
        text = text,
        authorId = authorId,
        status = mapStatusToString(status = status),
        timestamp = timestamp,
    )
}

internal fun MessageTable.toSM(): MessageSM? {
    return MessageSM(
        id = id,
        chatId = chatId,
        text = text,
        authorId = authorId,
        status = mapStringToStatus(status) ?: return null,
        timestamp = timestamp,
    )
}

private fun mapStatusToString(status: MessageSM.Status): String {
    return when (status) {
        MessageSM.Status.Created -> "Created"
        MessageSM.Status.Sent -> "Sent"
        MessageSM.Status.Received -> "Received"
    }
}

private fun mapStringToStatus(string: String): MessageSM.Status? {
    MessageSM.Status.entries.forEach { status ->
        if (mapStatusToString(status) == string) {
            return status
        }
    }

    return null
}