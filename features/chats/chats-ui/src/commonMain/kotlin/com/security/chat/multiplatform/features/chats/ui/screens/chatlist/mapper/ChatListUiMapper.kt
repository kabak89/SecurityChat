package com.security.chat.multiplatform.features.chats.ui.screens.chatlist.mapper

import com.security.chat.multiplatform.features.chats.domain.entity.ChatDescription
import com.security.chat.multiplatform.features.chats.ui.screens.chatlist.entity.ChatItem

internal fun ChatDescription.toUi(): ChatItem {
    return ChatItem(
        id = id,
        companionName = interlocutorName,
        nameAbbreviation = interlocutorName.take(2).uppercase(),
    )
}