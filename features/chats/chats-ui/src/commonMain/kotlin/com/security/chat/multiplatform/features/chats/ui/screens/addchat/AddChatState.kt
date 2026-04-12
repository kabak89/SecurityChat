package com.security.chat.multiplatform.features.chats.ui.screens.addchat

import androidx.compose.runtime.Immutable

@Immutable
internal data class AddChatState(
    val username: String,
    val isLoading: Boolean,
    val showNotFoundDialog: Boolean,
)