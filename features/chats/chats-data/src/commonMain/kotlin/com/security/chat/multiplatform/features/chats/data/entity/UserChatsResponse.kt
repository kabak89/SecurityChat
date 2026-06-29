package com.security.chat.multiplatform.features.chats.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UserChatsResponse(
    @SerialName("personalChats") val personalChats: List<PersonalChat>,
    @SerialName("groupChats") val groupChats: List<GroupChat>,
) {

    @Serializable
    internal data class PersonalChat(
        @SerialName("id") val id: String,
        @SerialName("firstUserId") val firstUserId: String,
        @SerialName("secondUserId") val secondUserId: String,
    )

    @Serializable
    internal data class GroupChat(
        @SerialName("id") val id: String,
        @SerialName("authorId") val authorId: String,
        @SerialName("participantIds") val participantIds: List<String>,
    )
}
