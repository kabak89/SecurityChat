package com.security.chat.multiplatform.features.chats.data.mapper

import com.security.chat.multiplatform.features.chats.data.entity.CreateGroupChatResponse
import com.security.chat.multiplatform.features.chats.data.entity.UserChatsResponse
import com.security.chat.multiplatform.features.chats.data.storage.entity.ChatSM
import com.security.chat.multiplatform.features.chats.domain.entity.Chat
import com.security.chat.multiplatform.features.chats.domain.entity.ChatMember

internal fun Chat.GroupChat.toSM(): ChatSM.GroupChat {
    return ChatSM.GroupChat(
        id = id,
        authorId = author.id,
        members = members.map { it.id },
    )
}

internal fun ChatSM.GroupChat.toDomain(
    members: List<ChatMember>,
    author: ChatMember,
): Chat.GroupChat {
    return Chat.GroupChat(
        id = id,
        author = author,
        members = members,
    )
}

internal fun UserChatsResponse.GroupChat.toDomain(
    members: List<ChatMember>,
    author: ChatMember,
): Chat.GroupChat {
    return Chat.GroupChat(
        id = id,
        author = author,
        members = members,
    )
}

internal fun CreateGroupChatResponse.toSM(): ChatSM.GroupChat {
    return ChatSM.GroupChat(
        id = chatId,
        authorId = authorId,
        members = participantIds,
    )
}