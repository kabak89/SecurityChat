package com.security.chat.multiplatform.features.chat.domain.repo

import com.security.chat.multiplatform.features.chat.domain.entity.Message

public interface ChatRepo {

    public suspend fun sendMessage(
        message: String,
        chatId: String,
    )

    public suspend fun fetchMessages(
        chatId: String,
    ): List<Message>

}