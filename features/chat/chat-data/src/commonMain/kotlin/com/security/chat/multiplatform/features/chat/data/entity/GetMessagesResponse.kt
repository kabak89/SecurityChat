package com.security.chat.multiplatform.features.chat.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GetMessagesResponse(
    @SerialName("messages") val messages: List<Message>,
) {

    @Serializable
    internal data class Message(
        @SerialName("id") val id: String,
        @SerialName("text") val text: String,
        @SerialName("authorId") val authorId: String,
    )

}