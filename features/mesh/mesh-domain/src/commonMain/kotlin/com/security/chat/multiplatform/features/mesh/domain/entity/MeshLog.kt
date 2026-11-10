package com.security.chat.multiplatform.features.mesh.domain.entity

public data class MeshLog(
    val id: Long,
    val recordedAtMillis: Long,
    val message: String,
)
