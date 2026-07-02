package com.security.chat.multiplatform.features.chats.ui.screens.chatlist.entity

import androidx.compose.runtime.Immutable

@Immutable
internal data class ChatItem(
    val id: String,
    val text: String,
    val abbreviation: String,
    val type: Type,
) {

    enum class Type {
        Personal,
        Group,
    }
}
