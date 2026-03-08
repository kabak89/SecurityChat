package com.security.chat.multiplatform.common.core.network.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@PublishedApi
@Serializable
internal data class SocketMessage(
    @PublishedApi
    @SerialName("id")
    internal val id: String,
    @PublishedApi
    @SerialName("payload")
    internal val payload: String,
)