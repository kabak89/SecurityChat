package com.security.chat.multiplatform.features.authorize.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SignInRequest(
    @SerialName("privateKeyHash") val privateKeyHash: String,
)
