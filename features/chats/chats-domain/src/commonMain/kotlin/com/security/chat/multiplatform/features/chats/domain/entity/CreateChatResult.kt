package com.security.chat.multiplatform.features.chats.domain.entity

public sealed interface CreateChatResult {

    public object UserNotFound : CreateChatResult

    public data class ChatCreated(
        val id: String,
    ) : CreateChatResult

}