package com.security.chat.multiplatform.features.chats.ui.screens.chatlist.entity

import androidx.compose.runtime.Immutable

@Immutable
internal data class ChatItem(
    val id: String,
    val companionName: String,
    val nameAbbreviation: String,
)
