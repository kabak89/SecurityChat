package com.security.chat.multiplatform.features.add_chat.ui

internal sealed interface AddChatEvent {

    data class PersonalChatCreated(
        val id: String,
    ) : AddChatEvent

    data class GroupChatCreated(
        val id: String,
    ) : AddChatEvent
}
