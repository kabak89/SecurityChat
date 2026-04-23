package com.security.chat.multiplatform.features.chats.data.storage.mapper

import com.security.chat.multiplatform.features.chats.data.storage.PersonalChatTable
import com.security.chat.multiplatform.features.chats.data.storage.entity.ChatSM

internal fun ChatSM.toTable(): PersonalChatTable {
    return PersonalChatTable(
        id = id,
        companionId = interlocutorId,
    )
}

internal fun PersonalChatTable.toSM(): ChatSM {
    return ChatSM(
        id = id,
        interlocutorId = companionId,
    )
}