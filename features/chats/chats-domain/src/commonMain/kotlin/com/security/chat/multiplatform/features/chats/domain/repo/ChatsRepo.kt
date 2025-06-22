package com.security.chat.multiplatform.features.chats.domain.repo

import com.security.chat.multiplatform.features.chats.domain.entity.ChatDescription
import com.security.chat.multiplatform.features.chats.domain.entity.CreateChatResult
import com.security.chat.multiplatform.features.chats.domain.entity.FindUserResult

public interface ChatsRepo {

    public suspend fun findUser(username: String): FindUserResult
    public suspend fun createChat(secondUserId: String): CreateChatResult
    public suspend fun getChatsList(): List<ChatDescription>

}