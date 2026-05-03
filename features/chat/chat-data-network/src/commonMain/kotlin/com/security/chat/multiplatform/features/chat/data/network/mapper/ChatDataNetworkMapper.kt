package com.security.chat.multiplatform.features.chat.data.network.mapper

import com.security.chat.multiplatform.features.chat.data.network.entity.ChatMessageNM
import com.security.chat.multiplatform.features.chat.data.network.entity.network.ChatMessage

internal fun ChatMessage.toNM(): ChatMessageNM {
    return ChatMessageNM(
        id = id,
        text = text,
        authorId = authorId,
        timestamp = timestamp,
    )
}