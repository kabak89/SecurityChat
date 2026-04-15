package com.security.chat.multiplatform.features.chats.data.mapper

import com.security.chat.multiplatform.features.chats.data.entity.ChatResponse
import com.security.chat.multiplatform.features.chats.data.storage.entity.ChatSM
import com.security.chat.multiplatform.features.chats.domain.entity.ChatDescription

internal fun ChatDescription.toSM(): ChatSM {
    return ChatSM(
        id = id,
        companionId = companionId,
    )
}

internal fun ChatSM.toDomain(
    companionName: String,
): ChatDescription {
    return ChatDescription(
        id = id,
        companionId = companionId,
        companionName = companionName,
    )
}

internal fun ChatResponse.toDomain(
    companionName: String,
    companionId: String,
): ChatDescription {
    return ChatDescription(
        id = id,
        companionId = companionId,
        companionName = companionName,
    )
}