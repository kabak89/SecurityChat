package com.security.chat.multiplatform.features.profile.ui.screens.main

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.domain.asLceState
import com.security.chat.multiplatform.common.core.domain.startOnSubscribe
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import com.security.chat.multiplatform.common.core.ui.mappers.toUiLceState
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogContent
import com.security.chat.multiplatform.features.profile.domain.ProfileModel
import com.security.chat.multiplatform.features.profile.ui.screens.main.entity.DialogContent
import com.security.chat.multiplatform.features.profile.ui.screens.main.mapper.updateProfileErrorMapper
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import securitychat.common.localization.generated.resources.common_close
import securitychat.common.localization.generated.resources.common_retry

internal class ProfileMainViewModel(
    private val profileModel: ProfileModel,
) : BaseViewModel<ProfileMainState, Unit>() {

    override fun onPostStart() {
        super.onPostStart()

        profileModel.fetchUserInfo.startOnSubscribe()

        profileModel.fetchUserInfo.jobFlow.asLceState().map { it.toUiLceState() }
            .onEach { fetchInfoState ->
                val dialogContent = when (fetchInfoState) {
                    is UiLceState.Loading,
                    is UiLceState.NotStarted,
                    is UiLceState.Ready,
                        -> null

                    is UiLceState.Error -> {
                        val alertDialogContent = AlertDialogContent(
                            title = fetchInfoState.error.title,
                            message = fetchInfoState.error.description,
                            positiveButtonText = resPrintableText(StringRes.common_retry),
                            negativeButtonText = resPrintableText(StringRes.common_close),
                        )

                        DialogContent(
                            errorDialogContent = alertDialogContent,
                            positiveAction = { profileModel.fetchUserInfo.startOnSubscribe() },
                            negativeAction = { updateState { it.copy(dialogContent = null) } },
                            dismissAction = { updateState { it.copy(dialogContent = null) } },
                        )
                    }
                }
                updateState {
                    it.copy(
                        fetchInfoState = fetchInfoState,
                        dialogContent = dialogContent,
                    )
                }
            }
            .launchIn(viewModelScope)

        profileModel.getProfileFlow()
            .filterNotNull()
            .onEach { profile ->
                updateState { it.copy(login = profile.name) }
            }
            .launchIn(viewModelScope)

        profileModel.getIsUpdateAvailableFlow()
            .onEach { isUpdateAvailable ->
                updateState { it.copy(loginChangeEnabled = isUpdateAvailable) }
            }
            .launchIn(viewModelScope)

        profileModel.updateUserInfo.jobFlow.asLceState()
            .map {
                it.toUiLceState(
                    errorMapper = ::updateProfileErrorMapper,
                )
            }
            .onEach { changeNameState ->
                val dialogContent = when (changeNameState) {
                    is UiLceState.Loading,
                    is UiLceState.NotStarted,
                    is UiLceState.Ready,
                        -> null

                    is UiLceState.Error -> {
                        val alertDialogContent = AlertDialogContent(
                            title = changeNameState.error.title,
                            message = changeNameState.error.description,
                            positiveButtonText = resPrintableText(StringRes.common_retry),
                            negativeButtonText = resPrintableText(StringRes.common_close),
                        )

                        DialogContent(
                            errorDialogContent = alertDialogContent,
                            positiveAction = { profileModel.updateUserInfo.startOnSubscribe() },
                            negativeAction = { updateState { it.copy(dialogContent = null) } },
                            dismissAction = { updateState { it.copy(dialogContent = null) } },
                        )
                    }
                }

                updateState {
                    it.copy(
                        changeNameState = changeNameState,
                        dialogContent = dialogContent,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun createInitialState(): ProfileMainState {
        return ProfileMainState(
            fetchInfoState = UiLceState.NotStarted,
            login = "",
            changeNameState = UiLceState.NotStarted,
            loginChangeEnabled = false,
            dialogContent = null,
        )
    }

    fun onUsernameTextChanged(newUsername: String) {
        profileModel.setName(newUsername)
    }

    fun onUpdateUsernameClicked() {
        profileModel.updateUserInfo.startOnSubscribe()
    }
}
