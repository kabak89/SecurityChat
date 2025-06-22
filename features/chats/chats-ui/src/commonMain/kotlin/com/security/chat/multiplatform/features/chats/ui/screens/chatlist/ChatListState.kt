package com.security.chat.multiplatform.features.chats.ui.screens.chatlist

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.features.chats.ui.screens.chatlist.entity.Chats

@Immutable
internal data class ChatListState(
    val chatListIsLoading: Boolean,
    val chats: Chats,
)