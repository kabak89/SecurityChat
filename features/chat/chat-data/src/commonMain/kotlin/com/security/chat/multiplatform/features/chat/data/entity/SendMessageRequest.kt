package com.security.chat.multiplatform.features.chat.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SendMessageRequest(
    @SerialName("id") val id: String,
    @SerialName("chatId") val chatId: String,
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("recipients") val recipients: List<RecipientCiphertext>,
)

@Serializable
internal data class RecipientCiphertext(
    @SerialName("recipientId") val recipientId: String,
    @SerialName("message") val message: String,
    @SerialName("key") val key: String,
)
