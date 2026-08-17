package com.security.chat.multiplatform.features.chats.data.common.entity

public data class ChatInfo(
    val id: String,
    val authorId: String,
    val participantIds: List<String>,
)
