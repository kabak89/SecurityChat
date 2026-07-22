package com.security.chat.multiplatform.features.chat.domain.entity

public data class ImageMessageDescriptor(
    val fileId: String,
    val localPath: String,
    val key: String,
    val recipients: List<String>,
)
