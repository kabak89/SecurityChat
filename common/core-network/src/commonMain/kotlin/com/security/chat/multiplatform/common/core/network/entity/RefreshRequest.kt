package com.security.chat.multiplatform.common.core.network.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RefreshRequest(
    @SerialName("refreshToken") val refreshToken: String,
)
