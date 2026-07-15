package com.security.chat.multiplatform.features.chat.data.common.mapper

import com.security.chat.multiplatform.features.chat.data.network.entity.ChatMessageNM
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM

internal suspend fun ChatMessageNM.toSM(
    chatId: String,
    decryptMessage: suspend (encryptedText: String, key: String) -> String,
    recipients: List<String>,
): MessageSM {
    return MessageSM(
        id = id,
        chatId = chatId,
        text = decryptMessage(text, key),
        authorId = authorId,
        //TODO
        status = MessageSM.Status.Received,
        timestamp = timestamp,
        recipients = recipients,
    )
}