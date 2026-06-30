package com.security.chat.multiplatform.features.chats.domain.entity

public data class FindUserResult(
    val userId: String,
    val login: String,
    val publicKey: String,
)