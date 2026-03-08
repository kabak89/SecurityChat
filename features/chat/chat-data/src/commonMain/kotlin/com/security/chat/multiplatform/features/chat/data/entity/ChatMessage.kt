package com.security.chat.multiplatform.features.chat.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ChatMessage(
    @SerialName("id") val id: String,
    @SerialName("text") val text: String,
    @SerialName("authorId") val authorId: String,
    @SerialName("timestamp") val timestamp: Long,
)