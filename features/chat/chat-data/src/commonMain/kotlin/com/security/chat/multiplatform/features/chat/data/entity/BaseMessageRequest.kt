package com.security.chat.multiplatform.features.chat.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface BaseMessageRequest {
    val id: String
    val chatId: String
    val timestamp: Long
    val recipients: List<RecipientCiphertext>
}

@Serializable
internal data class RecipientCiphertext(
    @SerialName("recipientId") val recipientId: String,
    @SerialName("key") val key: String,
)