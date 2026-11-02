package com.security.chat.multiplatform.features.chats.data.network.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class ChatInfoNM(
    @SerialName("id") val id: String,
    @SerialName("authorId") val authorId: String,
    @SerialName("participantIds") val participantIds: List<String>,
)
