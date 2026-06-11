package com.security.chat.multiplatform.common.core.network.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RefreshResponse(
    @SerialName("userId") val userId: String,
    @SerialName("accessToken") val accessToken: String,
    @SerialName("refreshToken") val refreshToken: String,
)
