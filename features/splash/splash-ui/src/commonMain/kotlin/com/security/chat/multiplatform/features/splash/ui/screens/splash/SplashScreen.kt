package com.security.chat.multiplatform.features.splash.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.core.ui.SingleEventEffect
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.splash.component.SplashComponent
import kotlinx.coroutines.flow.Flow

@Composable
public fun SplashScreen(
    component: SplashComponent,
) {
    Screen(component) { state: SplashState, vm: SplashViewModel ->
        SplashContent(
            modifier = Modifier
                .fillMaxSize(),
            state = state,
            events = vm.viewEvent,
            onUserNotAuthorized = component::onGoAuthorization,
            onUserAuthorized = component::onUserAuthorized,
        )
    }
}

@Composable
private fun SplashContent(
    modifier: Modifier = Modifier,
    state: SplashState,
    events: Flow<SplashEvent>,
    onUserNotAuthorized: () -> Unit,
    onUserAuthorized: () -> Unit,
) {
    SingleEventEffect(
        sideEffectFlow = events,
        collector = { event ->
            when (event) {
                SplashEvent.UserNotAuthorized -> onUserNotAuthorized()
                SplashEvent.UserAuthorized -> onUserAuthorized()
            }
        },
    )

    Box(
        modifier = modifier
            .background(AppTheme.colors.backgroundPrimary),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    color = AppTheme.colors.element,
                )
            }
        }
    }
}