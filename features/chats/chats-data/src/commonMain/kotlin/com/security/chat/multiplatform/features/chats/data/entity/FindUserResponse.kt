package com.security.chat.multiplatform.features.chats.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FindUserResponse(
    @SerialName("userId") val userId: String,
    @SerialName("login") val login: String,
    @SerialName("publicKey") val publicKey: String,
)
