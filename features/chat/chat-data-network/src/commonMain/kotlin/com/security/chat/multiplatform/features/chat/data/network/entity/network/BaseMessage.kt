package com.security.chat.multiplatform.features.chat.data.network.entity.network

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface BaseMessage {
    val id: String
    val key: String
    val authorId: String
    val timestamp: Long
}
