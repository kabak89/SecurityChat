package com.security.chat.multiplatform.features.chat.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GetPublicKeyResponse(
    @SerialName("publicKey") val publicKey: String,
)