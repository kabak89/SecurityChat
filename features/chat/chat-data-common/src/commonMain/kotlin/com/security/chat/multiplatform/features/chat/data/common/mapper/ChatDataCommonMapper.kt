package com.security.chat.multiplatform.features.chat.data.common.mapper

import com.security.chat.multiplatform.features.chat.data.network.entity.ChatMessageNM
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageRecipientSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM

internal suspend fun ChatMessageNM.toSM(
    chatId: String,
    decryptMessage: suspend (encryptedText: String, key: String) -> String,
    decryptKey: suspend (key: String) -> String,
    recipients: List<MessageRecipientSM>,
): MessageSM {
    return when (this) {
        is ChatMessageNM.Text -> {
            MessageSM.Text(
                id = id,
                chatId = chatId,
                text = decryptMessage(text, key),
                authorId = authorId,
                timestamp = timestamp,
                recipients = recipients,
            )
        }

        is ChatMessageNM.Image -> {
            MessageSM.Image(
                id = id,
                chatId = chatId,
                authorId = authorId,
                timestamp = timestamp,
                recipients = recipients,
                fileId = fileId,
                key = decryptKey(key),
                isDownloaded = false,
            )
        }
    }
}
