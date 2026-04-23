package com.security.chat.multiplatform.features.chat.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OnlineInfoMessage(
    @SerialName("userId") val userId: String,
    @SerialName("isOnline") val isOnline: Boolean,
)