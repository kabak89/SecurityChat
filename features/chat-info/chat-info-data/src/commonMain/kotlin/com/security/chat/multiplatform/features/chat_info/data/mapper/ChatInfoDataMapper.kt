package com.security.chat.multiplatform.features.chat_info.data.mapper

import com.security.chat.multiplatform.features.chat_info.data.entity.CreateGroupChatResponse
import com.security.chat.multiplatform.features.chats.data.common.entity.ChatInfo

internal fun CreateGroupChatResponse.toData(): ChatInfo {
    return ChatInfo(
        id = chatId,
        authorId = authorId,
        participantIds = participantIds,
    )
}