package com.security.chat.multiplatform.features.profile.ui.screens.deleteprofile

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.domain.asLceState
import com.security.chat.multiplatform.common.core.domain.startOnSubscribe
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState
import com.security.chat.multiplatform.common.core.ui.entity.isLoading
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import com.security.chat.multiplatform.common.core.ui.mappers.toUiLceState
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogContent
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor
import com.security.chat.multiplatform.features.profile.domain.DeleteProfileModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import securitychat.common.localization.generated.resources.common_close
import securitychat.common.localization.generated.resources.common_retry
import kotlin.time.Duration.Companion.seconds

internal class DeleteProfileViewModel(
    private val deleteProfileModel: DeleteProfileModel,
) : BaseViewModel<DeleteProfileState, DeleteProfileEvent>() {

    override fun onPostStart() {
        super.onPostStart()

        deleteProfileModel.deleteProfile.jobFlow.asLceState().map { it.toUiLceState() }
            .onEach { state ->
                updateState { it.copy(showLoading = state.isLoading) }

                if (state is UiLceState.Error) {
                    val content = AlertDialogContent(
                        title = state.error.title,
                        message = state.error.description,
                        positiveButtonText = resPrintableText(StringRes.common_retry),
                        negativeButtonText = resPrintableText(StringRes.common_close),
                    )
                    val alertDialogDescriptor = AlertDialogDescriptor(
                        content = content,
                        dismissAction = { updateState { it.copy(alertDialogDescriptor = null) } },
                        positiveAction = {
                            updateState { it.copy(alertDialogDescriptor = null) }
                            deleteProfileModel.deleteProfile.startOnSubscribe()
                        },
                        negativeAction = { updateState { it.copy(alertDialogDescriptor = null) } },
                    )
                    updateState { it.copy(alertDialogDescriptor = alertDialogDescriptor) }
                }
            }
            .launchIn(viewModelScope)
    }

    override fun createInitialState(): DeleteProfileState {
        return DeleteProfileState(
            showLoading = false,
            alertDialogDescriptor = null,
        )
    }

    fun onConfirmDeleteClicked() {
        if (currentViewState.showLoading) return

        viewModelScope.launch {
            updateState { it.copy(showLoading = true) }
            //to show animation
            delay(2.seconds)
            deleteProfileModel.deleteProfile.startOnSubscribe()
        }
    }
}
