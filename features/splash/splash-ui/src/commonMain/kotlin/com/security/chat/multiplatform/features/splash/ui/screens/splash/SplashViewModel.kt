package com.security.chat.multiplatform.features.splash.ui.screens.splash

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.domain.asLceState
import com.security.chat.multiplatform.common.core.domain.startOnSubscribe
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.features.splash.component.UserState
import com.security.chat.multiplatform.features.splash.domain.SplashModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class SplashViewModel(
    private val splashModel: SplashModel,
) : BaseViewModel<SplashState, SplashEvent>() {

    override fun onPostStart() {
        super.onPostStart()

        splashModel.fetchUserState.jobFlow.asLceState()
            .onEach { state ->
                updateState { it.copy(isLoading = state.isLoading) }
            }
            .launchIn(viewModelScope)

        splashModel.getUserStateFlow()
            .filterNotNull()
            .onEach { state ->
                sendEvent(
                    SplashEvent.UserStateReceived(
                        userState = UserState(
                            isAuthorized = state.isAuthorized,
                            isOnboardingPassed = state.isOnboardingPassed,
                        ),
                    ),
                )
            }
            .launchIn(viewModelScope)

        splashModel.fetchUserState.startOnSubscribe()
    }

    override fun createInitialState(): SplashState {
        return SplashState(
            isLoading = false,
        )
    }

}