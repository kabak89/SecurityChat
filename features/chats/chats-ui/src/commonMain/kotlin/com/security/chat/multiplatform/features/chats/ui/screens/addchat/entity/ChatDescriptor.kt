package com.security.chat.multiplatform.features.chats.ui.screens.addchat.entity

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface ChatDescriptor {

    val type: ChatType

    @Immutable
    data class Personal(
        val username: String,
        val isLoading: Boolean,
    ) : ChatDescriptor {
        override val type: ChatType = ChatType.Personal
    }

    @Immutable
    data class Group(
        val username: String,
        val isLoading: Boolean,
        val addedUsers: List<String>,
    ) : ChatDescriptor {
        override val type: ChatType = ChatType.Group
    }
}

internal enum class ChatType {
    Personal,
    Group
}
