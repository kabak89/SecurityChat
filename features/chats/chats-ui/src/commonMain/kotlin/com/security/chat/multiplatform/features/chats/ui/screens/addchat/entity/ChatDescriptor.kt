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
        val isFindButtonEnabled = username.isNotBlank()
    }

    @Immutable
    data class Group(
        val username: String,
        val searchInProgress: Boolean,
        val creationInProgress: Boolean,
        val addedUsers: List<AddedUser>,
    ) : ChatDescriptor {
        override val type: ChatType = ChatType.Group
        val isFindButtonEnabled = username.isNotBlank()
        val isCreateButtonEnabled = addedUsers.isNotEmpty()
        val smthIsLoading = searchInProgress || creationInProgress
    }
}

internal enum class ChatType {
    Personal,
    Group
}
