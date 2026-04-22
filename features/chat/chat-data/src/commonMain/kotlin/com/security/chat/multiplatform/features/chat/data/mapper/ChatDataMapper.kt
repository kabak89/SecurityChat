package com.security.chat.multiplatform.features.chat.data.mapper

import com.security.chat.multiplatform.features.chat.data.entity.ChatMessage
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.entity.MessageDirection

internal suspend fun ChatMessage.toDomain(
    decryptMessage: suspend (encryptedText: String) -> String,
    appOwnerId: String,
): Message {
    val direction = if (authorId == appOwnerId) {
        MessageDirection.Outgoing
    } else {
        MessageDirection.Incoming
    }

    return Message(
        id = id,
        text = decryptMessage(text),
        authorId = authorId,
        timestamp = timestamp,
        direction = direction,
    )
}

internal fun MessageSM.toDomain(
    appOwnerId: String,
): Message {
    val direction = if (authorId == appOwnerId) {
        MessageDirection.Outgoing
    } else {
        MessageDirection.Incoming
    }

    return Message(
        id = id,
        text = text,
        authorId = authorId,
        timestamp = timestamp,
        direction = direction,
    )
}

internal fun Message.toSM(
    chatId: String,
): MessageSM {
    return MessageSM(
        id = id,
        text = text,
        authorId = authorId,
        chatId = chatId,
        //TODO
        status = MessageSM.Status.Received,
        timestamp = timestamp,
    )
}