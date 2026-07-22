package com.security.chat.multiplatform.features.chat.data.network.entity.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal open class BaseMessage(
    @SerialName("id") open val id: String,
    @SerialName("key") open val key: String,
    @SerialName("authorId") open val authorId: String,
    @SerialName("timestamp") open val timestamp: Long,
)