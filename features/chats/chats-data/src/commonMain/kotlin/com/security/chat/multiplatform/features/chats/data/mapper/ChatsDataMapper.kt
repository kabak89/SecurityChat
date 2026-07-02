package com.security.chat.multiplatform.features.chats.data.mapper

import com.security.chat.multiplatform.features.chats.data.entity.UserChatsResponse
import com.security.chat.multiplatform.features.chats.data.storage.entity.ChatSM
import com.security.chat.multiplatform.features.chats.domain.entity.Chat
import com.security.chat.multiplatform.features.chats.domain.entity.ChatMember

internal fun Chat.PersonalChat.toSM(): ChatSM.PersonalChat {
    return ChatSM.PersonalChat(
        id = id,
        interlocutorId = companionId,
    )
}

internal fun Chat.GroupChat.toSM(): ChatSM.GroupChat {
    return ChatSM.GroupChat(
        id = id,
        members = members.map { it.id },
    )
}

internal fun ChatSM.PersonalChat.toDomain(
    interlocutorName: String,
): Chat.PersonalChat {
    return Chat.PersonalChat(
        id = id,
        companionId = interlocutorId,
        interlocutorName = interlocutorName,
    )
}

internal fun ChatSM.GroupChat.toDomain(
    members: List<ChatMember>,
): Chat.GroupChat {
    return Chat.GroupChat(
        id = id,
        members = members,
    )
}

internal fun UserChatsResponse.PersonalChat.toDomain(
    companionName: String,
    companionId: String,
): Chat.PersonalChat {
    return Chat.PersonalChat(
        id = id,
        companionId = companionId,
        interlocutorName = companionName,
    )
}

internal fun UserChatsResponse.GroupChat.toDomain(
    members: List<ChatMember>,
): Chat.GroupChat {
    return Chat.GroupChat(
        id = id,
        members = members,
    )
}