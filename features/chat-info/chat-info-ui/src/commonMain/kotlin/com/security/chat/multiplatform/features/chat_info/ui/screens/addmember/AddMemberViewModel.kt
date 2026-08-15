package com.security.chat.multiplatform.features.chat_info.ui.screens.addmember

import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.chat_info.component.api.AddMemberComponent

internal class AddMemberViewModel(
    private val params: AddMemberComponent,
) : BaseViewModel<AddMemberState, Unit>() {

    override fun createInitialState(): AddMemberState = AddMemberState(
        username = "",
        searchInProgress = false,
        addingInProgress = false,
        foundMembers = emptyList(),
    )

    fun onUsernameTextChanged(username: String) {
        updateState { it.copy(username = username) }
    }

    fun onFindClicked() {
        // TODO: implement search logic
    }

    fun onAddClicked() {
        // TODO: implement adding logic
    }

    fun onRemoveMemberClicked(memberId: String) {
        val memberToDelete = currentViewState.foundMembers.find { it.id == memberId }

        if (memberToDelete == null) {
            Log.e("member to delete not found: $memberId")
            return
        }

        val newMembers = currentViewState.foundMembers - memberToDelete
        updateState { it.copy(foundMembers = newMembers) }
    }
}
