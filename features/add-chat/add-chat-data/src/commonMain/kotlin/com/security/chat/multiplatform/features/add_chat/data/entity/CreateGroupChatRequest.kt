package com.security.chat.multiplatform.features.add_chat.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CreateGroupChatRequest(
    @SerialName("participantIds") val participantIds: List<String>,
)