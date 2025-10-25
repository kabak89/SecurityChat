package com.security.chat.multiplatform.features.chats.data.mapper

import com.security.chat.multiplatform.features.chats.data.storage.entity.ChatSM
import com.security.chat.multiplatform.features.chats.domain.entity.ChatDescription

internal fun ChatDescription.toSM(): ChatSM {
    return ChatSM(
        id = id,
        firstUserId = firstUserId,
        secondUserId = secondUserId,
    )
}