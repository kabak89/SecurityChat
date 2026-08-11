package com.security.chat.multiplatform.features.chats.domain.entity

public sealed interface Chat {

    public val id: String

    public data class GroupChat(
        override val id: String,
        val author: ChatMember,
        val members: List<ChatMember>,
    ) : Chat
}