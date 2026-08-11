package com.security.chat.multiplatform.features.chats.ui.screens.chatlist.mapper

import com.security.chat.multiplatform.features.chats.domain.entity.Chat
import com.security.chat.multiplatform.features.chats.ui.screens.chatlist.entity.ChatItem

internal fun Chat.toUi(): ChatItem {
    return when (this) {
        is Chat.GroupChat -> {
            val text = (listOf(author) + members).joinToString(separator = ", ") { it.username }

            ChatItem(
                id = id,
                text = text,
                abbreviation = text.take(2).uppercase(),
            )
        }
    }
}