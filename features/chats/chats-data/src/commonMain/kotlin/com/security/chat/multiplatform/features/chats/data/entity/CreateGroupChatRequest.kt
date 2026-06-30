package com.security.chat.multiplatform.features.chats.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CreateGroupChatRequest(
    @SerialName("participantIds") val participantIds: List<String>,
)
