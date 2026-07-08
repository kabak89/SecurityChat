package com.security.chat.multiplatform.features.chat.domain.entity

public data class Message(
    val id: String,
    val text: String,
    val author: MessageAuthor,
    val timestamp: Long,
    val direction: MessageDirection,
)
