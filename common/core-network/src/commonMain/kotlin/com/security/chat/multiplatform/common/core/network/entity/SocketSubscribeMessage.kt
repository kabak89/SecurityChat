package com.security.chat.multiplatform.common.core.network.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@PublishedApi
@Serializable
internal data class SocketSubscribeMessage(
    @PublishedApi
    @SerialName("id")
    internal val id: String,
    @PublishedApi
    @SerialName("type")
    internal val type: String,
    @PublishedApi
    @SerialName("payload")
    internal val payload: String,
)
