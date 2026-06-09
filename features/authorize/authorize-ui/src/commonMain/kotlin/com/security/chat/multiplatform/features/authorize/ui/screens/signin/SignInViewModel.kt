package com.security.chat.multiplatform.features.authorize.ui.screens.signin

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.domain.asLceState
import com.security.chat.multiplatform.common.core.domain.startOnSubscribe
import com.security.chat.multiplatform.common.core.error.NetworkError
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState
import com.security.chat.multiplatform.common.core.ui.entity.isLoading
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import com.security.chat.multiplatform.common.core.ui.mappers.toUiLceState
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogContent
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor
import com.security.chat.multiplatform.features.authorize.domain.SignInModel
import com.security.chat.multiplatform.features.authorize.ui.screens.signin.mapper.signInErrorMapper
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ru.kode.remo.successResults
import securitychat.common.localization.generated.resources.common_close
import securitychat.common.localization.generated.resources.common_retry

internal class SignInViewModel(
    private val signInModel: SignInModel,
) : BaseViewModel<SignInState, SignInEvent>() {

    override fun onPostStart() {
        super.onPostStart()

        signInModel.getStateFlow()
            .onEach { domainState ->
                updateState { oldState ->
                    oldState.copy(
                        username = domainState.username,
                        password = domainState.password,
                        isSignInEnabled = domainState.isSignInEnabled,
                    )
                }
            }
            .launchIn(viewModelScope)

        signInModel.signIn.jobFlow.asLceState().map { it.toUiLceState(::signInErrorMapper) }
            .onEach { state ->
                val isLoading = state.isLoading

                updateState { it.copy(isLoading = isLoading) }

                if (state is UiLceState.Error) {
                    val cause = state.error.cause

                    val alertDialogDescriptor = when {
                        cause is NetworkError &&
                                (cause.statusCode == 404 || cause.statusCode == 403) -> {
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
                                    signInModel.signIn.startOnSubscribe()
                                },
                            )
                        }
                    }
                    updateState { it.copy(alertDialogDescriptor = alertDialogDescriptor) }
                }
            }
            .launchIn(viewModelScope)

        signInModel.signIn.jobFlow.successResults()
            .onEach {
                sendEvent(SignInEvent.Authorized)
            }
            .launchIn(viewModelScope)
    }

    override fun createInitialState(): SignInState {
        return SignInState(
            username = "",
            password = "",
            isLoading = false,
            isSignInEnabled = false,
            alertDialogDescriptor = null,
            isOnboardingPassed = false,
        )
    }

    fun onUsernameTextChanged(newUsername: String) {
        signInModel.setUsername(newUsername)
    }

    fun onPasswordTextChanged(newPassword: String) {
        signInModel.setPassword(newPassword)
    }

    fun onSignInClicked() {
        signInModel.signIn.startOnSubscribe()
    }

}