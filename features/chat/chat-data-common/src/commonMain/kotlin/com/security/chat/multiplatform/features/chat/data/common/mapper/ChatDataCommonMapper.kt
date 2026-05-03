package com.security.chat.multiplatform.features.chat.data.common.mapper

import com.security.chat.multiplatform.features.chat.data.network.entity.ChatMessageNM
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.users.data.network.entity.UserNM
import com.security.chat.multiplatform.features.users.data.storage.entity.UserSM

internal fun UserNM.toSM(): UserSM {
    return UserSM(
        id = userId,
        publicKey = publicKey,
        name = name,
    )
}

internal suspend fun ChatMessageNM.toSM(
    chatId: String,
    decryptMessage: suspend (encryptedText: String) -> String,
): MessageSM {
    return MessageSM(
        id = id,
        chatId = chatId,
        text = decryptMessage(text),
        authorId = authorId,
        //TODO
        status = MessageSM.Status.Received,
        timestamp = timestamp,
    )
}