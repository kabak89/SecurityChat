package com.security.chat.multiplatform.features.chats.data.storage.entity

public sealed interface ChatSM {
    public val id: String

    public data class GroupChat(
        override val id: String,
        val authorId: String,
        val members: List<String>,
    ) : ChatSM
}
