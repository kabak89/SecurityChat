package com.security.chat.multiplatform.features.chat.data.network.entity.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ChatSubscribeMessage(
    @SerialName("chatId") val chatId: String,
    @SerialName("authorId") val authorId: String,
    @SerialName("deviceId") val deviceId: String,
)