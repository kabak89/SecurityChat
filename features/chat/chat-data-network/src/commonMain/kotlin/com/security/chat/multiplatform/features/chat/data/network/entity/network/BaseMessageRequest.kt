package com.security.chat.multiplatform.features.chat.data.network.entity.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public sealed interface BaseMessageRequest {
    public val id: String
    public val chatId: String
    public val timestamp: Long
    public val replyToMessageId: String?
    public val recipients: List<RecipientCiphertext>
}

@Serializable
public data class RecipientCiphertext(
    @SerialName("recipientId") val recipientId: String,
    @SerialName("key") val key: String,
)
