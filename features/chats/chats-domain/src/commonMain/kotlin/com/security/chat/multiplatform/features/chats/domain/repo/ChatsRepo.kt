package com.security.chat.multiplatform.features.chats.domain.repo

import com.security.chat.multiplatform.features.chats.domain.entity.Chat
import kotlinx.coroutines.flow.Flow

public interface ChatsRepo {
    public fun getChatsListFlow(): Flow<List<Chat>>
    public suspend fun fetchChatsList()
    public fun isConnectedToInternetFlow(): Flow<Boolean>
}
