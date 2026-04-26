package com.security.chat.multiplatform.features.chat.domain.repo

import androidx.paging.PagingData
import com.security.chat.multiplatform.features.chat.domain.entity.Interlocutor
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

    public fun getMessagesPager(chatId: String): Flow<PagingData<Message>>

    public suspend fun subscribeToNewMessages(chatId: String)
    public suspend fun fetchCompanionInfo(chatId: String)
    public fun getInterlocutorInfoFlow(chatId: String): Flow<Interlocutor?>
    public suspend fun setUserOnline()
}