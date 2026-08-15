package com.security.chat.multiplatform.features.chat_info.ui.screens.addmember

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.features.chat_info.ui.screens.addmember.entity.FoundMember

@Immutable
internal data class AddMemberState(
    val username: String,
    val searchInProgress: Boolean,
    val addingInProgress: Boolean,
    val foundMembers: List<FoundMember>,
) {
    val smthIsLoading: Boolean = searchInProgress || addingInProgress
    val addingIsEnabled = foundMembers.isNotEmpty()
}