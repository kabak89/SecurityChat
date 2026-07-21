package com.security.chat.multiplatform.features.chat.domain.entity

public data class ImageMessageDescriptor(
    val file: FileDescriptor,
    /**
     * userId <-> encrypted keys association
     */
    val keys: Map<String, String>,
)
