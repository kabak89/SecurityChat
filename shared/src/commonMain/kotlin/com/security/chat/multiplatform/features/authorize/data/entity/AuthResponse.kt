package com.security.chat.multiplatform.features.authorize.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    @SerialName("userId") val userId: String,
)