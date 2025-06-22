package com.security.chat.multiplatform.features.chats.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ChatResponse(
    @SerialName("id") val id: String,
    @SerialName("firstUserId") val firstUserId: String,
    @SerialName("secondUserId") val secondUserId: String,
)
