package com.security.chat.multiplatform.features.chats.ui.screens.addchat

internal sealed interface AddChatEvent {

    data class ChatCreated(
        val id: String,
    ) : AddChatEvent

}