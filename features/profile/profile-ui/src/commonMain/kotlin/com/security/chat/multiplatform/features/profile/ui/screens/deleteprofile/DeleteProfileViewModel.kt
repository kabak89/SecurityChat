package com.security.chat.multiplatform.features.profile.ui.screens.deleteprofile

import com.security.chat.multiplatform.common.core.ui.BaseViewModel

internal class DeleteProfileViewModel(

) : BaseViewModel<DeleteProfileState, DeleteProfileEvent>() {

    override fun createInitialState(): DeleteProfileState {
        return DeleteProfileState(
            showLoading = false,
        )
    }

    fun onDeleteClicked() {

    }

    fun onDismissConfirmDialog() {

    }

    fun onConfirmDeleteClicked() {

    }
}
