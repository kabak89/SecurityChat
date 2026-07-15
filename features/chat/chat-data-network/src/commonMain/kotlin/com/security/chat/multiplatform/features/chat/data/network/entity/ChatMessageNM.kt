package com.security.chat.multiplatform.features.chat.data.network.entity

public data class ChatMessageNM(
    val id: String,
    val text: String,
    val key: String,
    val authorId: String,
    val timestamp: Long,
)
