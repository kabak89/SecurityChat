package com.security.chat.multiplatform.features.user.data.network.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UpdateProfileRequest(
    @SerialName("login") val login: String,
)
