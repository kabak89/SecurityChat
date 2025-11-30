package com.security.chat.multiplatform.features.chat.data.mapper

import com.security.chat.multiplatform.features.chat.data.entity.GetMessagesResponse
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.domain.entity.Message

internal suspend fun GetMessagesResponse.Message.toDomain(
    decryptMessage: suspend (encryptedText: String) -> String,
): Message {
    return Message(
        id = id,
        text = decryptMessage(text),
        authorId = authorId,
        timestamp = timestamp,
    )
}

internal fun MessageSM.toDomain(): Message {
    return Message(
        id = id,
        text = text,
        authorId = authorId,
        timestamp = timestamp,
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