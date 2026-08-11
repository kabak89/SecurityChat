package com.security.chat.multiplatform.features.add_chat.ui.entity

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface ChatDescriptor {

    @Immutable
    data class Group(
        val username: String,
        val searchInProgress: Boolean,
        val creationInProgress: Boolean,
        val addedUsers: List<AddedUser>,
    ) : ChatDescriptor {
        val isFindButtonEnabled = username.isNotBlank()
        val isCreateButtonEnabled = addedUsers.isNotEmpty()
        val smthIsLoading = searchInProgress || creationInProgress
    }
}
