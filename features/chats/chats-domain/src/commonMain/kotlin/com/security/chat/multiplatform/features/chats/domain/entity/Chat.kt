package com.security.chat.multiplatform.features.chats.domain.entity

public data class Chat(
    val id: String,
    val author: ChatMember,
    val members: List<ChatMember>,
    val unreadCount: Long,
)