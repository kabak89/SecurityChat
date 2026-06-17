package com.security.chat.multiplatform.features.profile.ui.screens.deleteprofile

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

internal class DeleteProfileViewModel(

) : BaseViewModel<DeleteProfileState, DeleteProfileEvent>() {

    override fun createInitialState(): DeleteProfileState {
        return DeleteProfileState(
            showLoading = false,
        )
    }

    fun onConfirmDeleteClicked() {
        //TODO add model call
        viewModelScope.launch {
            updateState { it.copy(showLoading = true) }
            delay(2.seconds)
            updateState { it.copy(showLoading = false) }
        }
    }
}
