package com.security.chat.multiplatform.features.add_chat.ui

internal sealed interface AddChatEvent {

    data class GroupChatCreated(
        val id: String,
    ) : AddChatEvent
}
