package com.security.chat.multiplatform.features.chat.data.network.entity.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SendMessageResponse(
    @SerialName("acceptedRecipientIds") val acceptedRecipientIds: List<String>,
)
