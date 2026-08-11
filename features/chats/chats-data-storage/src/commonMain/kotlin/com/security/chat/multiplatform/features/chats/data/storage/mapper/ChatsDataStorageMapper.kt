package com.security.chat.multiplatform.features.chats.data.storage.mapper

import com.security.chat.multiplatform.features.chats.data.storage.GroupChatTable
import com.security.chat.multiplatform.features.chats.data.storage.entity.ChatSM

internal fun ChatSM.GroupChat.toTable(): GroupChatTable {
    return GroupChatTable(
        id = id,
        authorId = authorId,
    )
}