package com.security.chat.multiplatform.features.chats.data.common.entity.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ChatDetailsResponse(
    @SerialName("id") val id: String,
    @SerialName("authorId") val authorId: String,
    @SerialName("participantIds") val participantIds: List<String>,
)