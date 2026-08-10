package com.security.chat.multiplatform.features.add_chat.domain.repo

import com.security.chat.multiplatform.features.add_chat.domain.entity.CreateChatResult
import com.security.chat.multiplatform.features.add_chat.domain.entity.FindUserResult

public interface AddChatRepo {
    public suspend fun findUser(username: String): FindUserResult
    public suspend fun createPersonalChat(secondUserId: String): CreateChatResult.PersonalChatCreated
    public suspend fun getUserId(): String
    public suspend fun createGroupChat(members: List<String>): CreateChatResult.GroupChatCreated
    public suspend fun refreshChatsList()
}
