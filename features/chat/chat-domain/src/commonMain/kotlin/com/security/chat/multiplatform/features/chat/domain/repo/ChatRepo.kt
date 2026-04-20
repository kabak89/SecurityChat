package com.security.chat.multiplatform.features.chat.domain.repo

import com.security.chat.multiplatform.features.chat.domain.entity.Message
import kotlinx.coroutines.flow.Flow

public interface ChatRepo {

    public suspend fun saveMessage(
        message: String,
        chatId: String,
    )

    public suspend fun uploadMessages(chatId: String)

    public suspend fun fetchMessages(
        chatId: String,
    )

    public fun getMessagesFlow(chatId: String): Flow<List<Message>>

    public suspend fun subscribeToNewMessages(chatId: String)
}