package com.security.chat.multiplatform.features.chats.data.storage.mapper

import com.security.chat.multiplatform.features.chats.data.storage.ChatTable
import com.security.chat.multiplatform.features.chats.data.storage.entity.ChatSM

internal fun ChatSM.toTable(): ChatTable {
    return ChatTable(
        id = id,
        firstUserId = firstUserId,
        secondUserId = secondUserId,
    )
}

internal fun ChatTable.toSM(): ChatSM {
    return ChatSM(
        id = id,
        firstUserId = firstUserId,
        secondUserId = secondUserId,
    )
}