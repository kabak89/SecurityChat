package com.security.chat.multiplatform.features.chat.domain.repo

public interface ChatRepo {

    public suspend fun sendMessage(
        message: String,
        chatId: String,
    )

}