package com.security.chat.multiplatform.features.splash.ui.screens.splash

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.domain.asLceState
import com.security.chat.multiplatform.common.core.domain.startOnSubscribe
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.features.splash.domain.SplashModel
import com.security.chat.multiplatform.features.splash.domain.entity.UserState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SplashViewModel(
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
            .onEach { state ->
                when (state) {
                    UserState.Unknown -> Unit
                    UserState.Authorized -> sendEvent(SplashEvent.UserAuthorized)
                    UserState.NotAuthorized -> sendEvent(SplashEvent.UserNotAuthorized)
                }
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