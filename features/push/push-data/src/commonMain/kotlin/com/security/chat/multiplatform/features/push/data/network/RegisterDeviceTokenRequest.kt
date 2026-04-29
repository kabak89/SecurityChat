package com.security.chat.multiplatform.features.push.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RegisterDeviceTokenRequest(
    @SerialName("userId") val userId: String,
    @SerialName("token") val token: String,
    @SerialName("platform") val platform: String,
)
