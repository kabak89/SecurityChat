package com.security.chat.multiplatform.features.authorize.ui.screens.signup

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
import com.security.chat.multiplatform.features.authorize.domain.SignUpModel
import com.security.chat.multiplatform.features.authorize.ui.screens.signup.mapper.isUsernameAlreadyExists
import com.security.chat.multiplatform.features.authorize.ui.screens.signup.mapper.signUpErrorMapper
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ru.kode.remo.successResults
import securitychat.common.localization.generated.resources.common_close
import securitychat.common.localization.generated.resources.common_retry

internal class SignUpViewModel(
    private val signUpModel: SignUpModel,
) : BaseViewModel<SignUpState, SignUpEvent>() {

    override fun onPostStart() {
        super.onPostStart()

        signUpModel.getStateFlow()
            .onEach { newState ->
                updateState {
                    it.copy(
                        username = newState.username,
                        nextButtonEnabled = newState.formFilled,
                    )
                }
            }
            .launchIn(viewModelScope)

        signUpModel.signUp.jobFlow.asLceState().map { it.toUiLceState(::signUpErrorMapper) }
            .onEach { state ->
                updateState { it.copy(isLoading = state.isLoading) }

                if (state is UiLceState.Error) {
                    val cause = state.error.cause

                    val alertDialogDescriptor = when {
                        cause.isUsernameAlreadyExists() -> {
                            val content = AlertDialogContent(
                                title = state.error.title,
                                message = state.error.description,
                                positiveButtonText = resPrintableText(StringRes.common_close),
                            )
                            AlertDialogDescriptor(
                                content = content,
                                dismissAction = {
                                    updateState { it.copy(alertDialogDescriptor = null) }
                                },
                                positiveAction = {
                                    updateState { it.copy(alertDialogDescriptor = null) }
                                },
                            )
                        }

                        else -> {
                            val content = AlertDialogContent(
                                title = state.error.title,
                                message = state.error.description,
                                positiveButtonText = resPrintableText(StringRes.common_close),
                                negativeButtonText = resPrintableText(StringRes.common_retry),
                            )
                            AlertDialogDescriptor(
                                content = content,
                                dismissAction = {
                                    updateState { it.copy(alertDialogDescriptor = null) }
                                },
                                positiveAction = {
                                    updateState { it.copy(alertDialogDescriptor = null) }
                                },
                                negativeAction = {
                                    updateState { it.copy(alertDialogDescriptor = null) }
                                    signUpModel.signUp.startOnSubscribe()
                                },
                            )
                        }
                    }
                    updateState { it.copy(alertDialogDescriptor = alertDialogDescriptor) }
                }
            }
            .launchIn(viewModelScope)

        signUpModel.signUp.jobFlow.successResults()
            .onEach {
                sendEvent(SignUpEvent.SuccessSignUp)
            }
            .launchIn(viewModelScope)
    }

    override fun createInitialState(): SignUpState {
        return SignUpState(
            username = "",
            isLoading = false,
            nextButtonEnabled = false,
            isOnboardingPassed = false,
            alertDialogDescriptor = null,
        )
    }

    fun onUsernameTextChanged(newUsername: String) {
        signUpModel.setUsername(newUsername)
    }

    fun onSignUpClicked() {
        signUpModel.signUp.startOnSubscribe()
    }
}