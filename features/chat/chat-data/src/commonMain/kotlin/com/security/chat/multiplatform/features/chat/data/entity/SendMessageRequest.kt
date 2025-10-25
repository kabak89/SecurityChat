package com.security.chat.multiplatform.features.chat.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SendMessageRequest(
    @SerialName("id") val id: String,
    @SerialName("creatorUserId") val authorId: String,
    @SerialName("chatId") val chatId: String,
    @SerialName("message") val message: String,
)
