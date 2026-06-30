package com.security.chat.multiplatform.features.chats.ui.screens.addchat

internal sealed interface AddChatEvent {

    data class PersonalChatCreated(
        val id: String,
    ) : AddChatEvent

    data class GroupChatCreated(
        val id: String,
    ) : AddChatEvent

}