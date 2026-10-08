package com.security.chat.multiplatform.common.core.network.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@PublishedApi
@Serializable
internal data class SocketSendMessage(
    @PublishedApi
    @SerialName("type")
    internal val type: String,
    @PublishedApi
    @SerialName("payload")
    internal val payload: String,
)
