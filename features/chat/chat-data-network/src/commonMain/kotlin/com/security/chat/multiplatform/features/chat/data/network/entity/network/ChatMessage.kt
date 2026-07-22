package com.security.chat.multiplatform.features.chat.data.network.entity.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ChatMessage(
    @SerialName("type") val type: String,
    @SerialName("message") val message: String,
)