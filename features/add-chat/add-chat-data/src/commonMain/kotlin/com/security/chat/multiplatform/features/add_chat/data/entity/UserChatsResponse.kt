package com.security.chat.multiplatform.features.add_chat.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UserChatsResponse(
    @SerialName("groupChats") val groupChats: List<GroupChat>,
) {

    @Serializable
    internal data class GroupChat(
        @SerialName("id") val id: String,
        @SerialName("authorId") val authorId: String,
        @SerialName("participantIds") val participantIds: List<String>,
    )
}