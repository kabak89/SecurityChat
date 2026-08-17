package com.security.chat.multiplatform.features.chats.data.common.mapper

import com.security.chat.multiplatform.features.chats.data.common.entity.ChatInfo
import com.security.chat.multiplatform.features.chats.data.storage.entity.ChatSM

internal fun ChatInfo.toSM(): ChatSM {
    return ChatSM.GroupChat(
        id = id,
        authorId = authorId,
        members = participantIds,
    )
}