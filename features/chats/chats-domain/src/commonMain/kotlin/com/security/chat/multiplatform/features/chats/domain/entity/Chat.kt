package com.security.chat.multiplatform.features.chats.domain.entity

public sealed interface Chat {

    public val id: String

    public data class PersonalChat(
        override val id: String,
        val companionId: String,
        val interlocutorName: String,
    ) : Chat

    public data class GroupChat(
        override val id: String,
        val members: List<ChatMember>,
    ) : Chat
}