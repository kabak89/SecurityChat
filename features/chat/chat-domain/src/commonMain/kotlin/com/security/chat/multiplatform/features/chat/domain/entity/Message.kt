package com.security.chat.multiplatform.features.chat.domain.entity

public data class Message(
    val id: String,
    val text: String,
    val authorId: String,
    val timestamp: Long,
)
