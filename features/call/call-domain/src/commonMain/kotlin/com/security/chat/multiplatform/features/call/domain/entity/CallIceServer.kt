package com.security.chat.multiplatform.features.call.domain.entity

public data class CallIceServer(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null,
)
