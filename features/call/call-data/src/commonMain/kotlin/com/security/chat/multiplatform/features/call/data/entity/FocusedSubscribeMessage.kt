package com.security.chat.multiplatform.features.call.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FocusedSubscribeMessage(
    @SerialName("chatId") val chatId: String,
)
