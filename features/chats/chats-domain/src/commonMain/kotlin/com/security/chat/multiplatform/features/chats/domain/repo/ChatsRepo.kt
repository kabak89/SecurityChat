package com.security.chat.multiplatform.features.chats.domain.repo

import com.security.chat.multiplatform.features.chats.domain.entity.ChatDescription
import com.security.chat.multiplatform.features.chats.domain.entity.CreateChatResult
import com.security.chat.multiplatform.features.chats.domain.entity.FindUserResult
import kotlinx.coroutines.flow.Flow

public interface ChatsRepo {
    public suspend fun findUser(username: String): FindUserResult
    public suspend fun createPersonalChat(secondUserId: String): CreateChatResult.PersonalChatCreated
    public fun getChatsListFlow(): Flow<List<ChatDescription>>
    public suspend fun fetchChatsList()
    public fun isConnectedToInternetFlow(): Flow<Boolean>
    public suspend fun getUserId(): String
    public suspend fun createGroupChat(members: List<String>): CreateChatResult.GroupChatCreated
}