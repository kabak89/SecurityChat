package com.security.chat.multiplatform.features.authorize.ui.screens.signup

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.domain.asLceState
import com.security.chat.multiplatform.common.core.domain.startOnSubscribe
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.authorize.domain.SignUpModel
import com.security.chat.multiplatform.features.authorize.domain.entity.SignUpResult
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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

        signUpModel.signUp.jobFlow.asLceState()
            .onEach { state ->
                updateState { it.copy(isLoading = state.isLoading) }
            }
            .launchIn(viewModelScope)

        signUpModel.getResultFlow()
            .filterNotNull()
            .onEach { result ->
                when (result) {
                    SignUpResult.LoginAlreadyExists -> {
                        //TODO
                        Log.d { "LoginAlreadyExists" }
                    }

                    SignUpResult.Success -> {
                        sendEvent(SignUpEvent.SuccessSignUp)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    override fun createInitialState(): SignUpState {
        return SignUpState(
            username = "",
            isLoading = false,
            nextButtonEnabled = false,
            isOnboardingPassed = false,
        )
    }

    fun onUsernameTextChanged(newUsername: String) {
        signUpModel.setUsername(newUsername)
    }

    fun onSignUpClicked() {
        signUpModel.signUp.startOnSubscribe()
    }
}