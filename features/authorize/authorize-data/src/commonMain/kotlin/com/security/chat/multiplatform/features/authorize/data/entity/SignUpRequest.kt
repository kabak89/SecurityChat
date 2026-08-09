package com.security.chat.multiplatform.features.authorize.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SignUpRequest(
    @SerialName("login") val login: String,
    @SerialName("publicKey") val publicKey: String,
    @SerialName("privateKeyHash") val privateKeyHash: String,
    @SerialName("deviceId") val deviceId: String,
    @SerialName("deviceName") val deviceName: String,
)
