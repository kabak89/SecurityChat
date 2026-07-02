package com.security.chat.multiplatform.features.chats.data.storage.mapper

import com.security.chat.multiplatform.features.chats.data.storage.GroupChatTable
import com.security.chat.multiplatform.features.chats.data.storage.PersonalChatTable
import com.security.chat.multiplatform.features.chats.data.storage.entity.ChatSM

internal fun ChatSM.PersonalChat.toTable(): PersonalChatTable {
    return PersonalChatTable(
        id = id,
        companionId = interlocutorId,
    )
}

internal fun ChatSM.GroupChat.toTable(): GroupChatTable {
    return GroupChatTable(
        id = id,
    )
}

internal fun PersonalChatTable.toSM(): ChatSM.PersonalChat {
    return ChatSM.PersonalChat(
        id = id,
        interlocutorId = companionId,
    )
}