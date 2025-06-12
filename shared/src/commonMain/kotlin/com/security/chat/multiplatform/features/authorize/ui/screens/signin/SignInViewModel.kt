package com.security.chat.multiplatform.features.authorize.ui.screens.signin

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.domain.asLceState
import com.security.chat.multiplatform.common.core.domain.startOnSubscribe
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.features.authorize.domain.SignInModel
import com.security.chat.multiplatform.features.authorize.domain.entity.SignInResult
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SignInViewModel(
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
                    )
                }
            }
            .launchIn(viewModelScope)

        signInModel.getAuthResultFlow()
            .filterNotNull()
            .onEach { result ->
                when (result) {
                    SignInResult.Success -> sendEvent(SignInEvent.Authorized)
                    SignInResult.UserNotExists -> {
                        //TODO
                        println("UserNotExists")
                    }

                    SignInResult.WrongPassword -> {
                        //TODO
                        println("WrongPassword")
                    }
                }
            }
            .launchIn(viewModelScope)

        signInModel.signIn.jobFlow.asLceState()
            .onEach { state ->
                val isLoading = state.isLoading

                updateState { it.copy(isLoading = isLoading) }

                val error = state.error

                if (error != null) {
                    //TODO
                }
            }
            .launchIn(viewModelScope)
    }

    override fun createInitialState(): SignInState {
        return SignInState(
            username = "",
            password = "",
            isLoading = false,
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