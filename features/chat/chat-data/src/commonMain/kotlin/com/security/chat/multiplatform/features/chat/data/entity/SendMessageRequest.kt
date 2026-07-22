package com.security.chat.multiplatform.features.chat.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SendMessageRequest(
    @SerialName("type") val type: String,
    @SerialName("message") val message: String,
)
