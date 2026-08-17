package com.security.chat.multiplatform.features.chat_info.domain.repository

import com.security.chat.multiplatform.features.chat_info.domain.entity.ChatMember
import kotlinx.coroutines.flow.Flow

public interface ChatInfoRepository {

    public suspend fun getCurrentMembersFlow(chatId: String): Flow<List<ChatMember>>
    public suspend fun searchMember(username: String): ChatMember
    public suspend fun fetchChatInfo(chatId: String)
    public suspend fun addMembers(chatId: String, memberIds: List<String>)
}
