package com.security.chat.multiplatform.features.chat_info.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AddGroupChatParticipantsRequest(
    @SerialName("chatId") val chatId: String,
    @SerialName("participantIds") val participantIds: List<String>,
)
