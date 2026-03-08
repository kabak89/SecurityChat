package com.security.chat.multiplatform.features.chat.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ChatSubscribeMessage(
    @SerialName("chatId") val chatId: String,
    @SerialName("authorId") val authorId: String,
)
