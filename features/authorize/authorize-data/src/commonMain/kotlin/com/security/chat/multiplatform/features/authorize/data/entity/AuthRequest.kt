package com.security.chat.multiplatform.features.authorize.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AuthRequest(
    @SerialName("login") val login: String,
    @SerialName("passwordHash") val passwordHash: String,
    @SerialName("publicKey") val publicKey: String,
)
