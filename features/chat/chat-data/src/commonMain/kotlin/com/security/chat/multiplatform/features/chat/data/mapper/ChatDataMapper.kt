package com.security.chat.multiplatform.features.chat.data.mapper

import com.security.chat.multiplatform.features.chat.data.entity.GetMessagesResponse
import com.security.chat.multiplatform.features.chat.domain.entity.Message

internal suspend fun GetMessagesResponse.Message.toDomain(
    decryptMessage: suspend (encryptedText: String) -> String,
): Message {
    return Message(
        id = id,
        text = decryptMessage(text),
        authorId = authorId,
    )
}