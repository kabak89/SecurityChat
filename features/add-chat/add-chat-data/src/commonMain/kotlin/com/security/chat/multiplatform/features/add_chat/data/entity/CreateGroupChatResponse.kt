package com.security.chat.multiplatform.features.add_chat.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CreateGroupChatResponse(
    @SerialName("chatId") val chatId: String,
    @SerialName("authorId") val authorId: String,
    @SerialName("participantIds") val participantIds: List<String>,
)