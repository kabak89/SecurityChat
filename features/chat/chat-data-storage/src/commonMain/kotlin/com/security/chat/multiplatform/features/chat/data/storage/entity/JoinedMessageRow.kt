package com.security.chat.multiplatform.features.chat.data.storage.entity

internal data class JoinedMessageRow(
    val id: String,
    val chatId: String,
    val authorId: String,
    val status: String,
    val timestamp: Long,
    val type: String,
    val text: String?,
    val fileId: String?,
    val key: String?,
    val localPath: String?,
)
