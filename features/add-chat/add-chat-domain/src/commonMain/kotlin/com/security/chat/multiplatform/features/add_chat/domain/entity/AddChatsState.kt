package com.security.chat.multiplatform.features.add_chat.domain.entity

public data class AddChatsState(
    val personalChatUsername: String,
    val groupChatUsername: String,
    val chatMembers: List<ChatMember>,
)
