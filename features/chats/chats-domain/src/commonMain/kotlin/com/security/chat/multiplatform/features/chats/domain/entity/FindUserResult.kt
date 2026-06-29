package com.security.chat.multiplatform.features.chats.domain.entity

public sealed interface FindUserResult {

    public data class UserFound(
        val userId: String,
        val login: String,
        val publicKey: String,
    ) : FindUserResult
}