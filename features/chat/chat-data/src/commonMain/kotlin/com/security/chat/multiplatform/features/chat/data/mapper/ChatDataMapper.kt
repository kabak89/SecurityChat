package com.security.chat.multiplatform.features.chat.data.mapper

import com.security.chat.multiplatform.features.chat.data.entity.GetMessagesResponse
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal suspend fun GetMessagesResponse.Message.toDomain(
    decryptMessage: suspend (encryptedText: String) -> String,
): Message {
    return Message(
        //TODO
        id = Uuid.random().toString(),
        text = decryptMessage(text),
    )
}