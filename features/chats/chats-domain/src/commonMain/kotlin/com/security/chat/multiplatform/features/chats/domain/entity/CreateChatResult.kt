package com.security.chat.multiplatform.features.chats.domain.entity

public sealed interface CreateChatResult {

    public data class PersonalChatCreated(
        val id: String,
    ) : CreateChatResult

    public data class GroupChatCreated(
        val id: String,
    ) : CreateChatResult
}