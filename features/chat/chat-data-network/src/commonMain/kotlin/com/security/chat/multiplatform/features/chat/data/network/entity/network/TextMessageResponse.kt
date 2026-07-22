package com.security.chat.multiplatform.features.chat.data.network.entity.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TextMessageResponse(
    @SerialName("id") override val id: String,
    @SerialName("key") override val key: String,
    @SerialName("authorId") override val authorId: String,
    @SerialName("timestamp") override val timestamp: Long,
    @SerialName("text") val text: String,
) : BaseMessage
