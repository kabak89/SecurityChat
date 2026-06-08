package com.security.chat.multiplatform.features.splash.ui.screens.splash

import com.security.chat.multiplatform.features.splash.component.UserState

internal sealed interface SplashEvent {

    data class UserStateReceived(
        val userState: UserState,
    ) : SplashEvent
}