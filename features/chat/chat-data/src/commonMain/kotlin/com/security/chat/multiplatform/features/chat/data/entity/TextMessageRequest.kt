package com.security.chat.multiplatform.features.chat.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TextMessageRequest(
    @SerialName("id") override val id: String,
    @SerialName("chatId") override val chatId: String,
    @SerialName("timestamp") override val timestamp: Long,
    @SerialName("recipients") override val recipients: List<RecipientCiphertext>,
    @SerialName("message") val message: String,
) : BaseMessageRequest