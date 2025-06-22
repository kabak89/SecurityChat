package com.security.chat.multiplatform.features.chats.ui.screens.chatlist.entity

import androidx.compose.runtime.Immutable

@Immutable
internal data class Chats(
    val items: List<ChatItem>,
) {

    companion object {
        val EMPTY = Chats(
            items = emptyList(),
        )
    }

}
