package com.security.chat.multiplatform.features.chat.data.network.entity.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MessagesReceivedRequest(
    @SerialName("chatId") val chatId: String,
    @SerialName("messageIds") val messageIds: List<String>,
    @SerialName("deviceId") val deviceId: String,
)
