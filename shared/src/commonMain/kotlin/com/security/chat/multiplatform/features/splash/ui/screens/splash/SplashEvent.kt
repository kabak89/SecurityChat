package com.security.chat.multiplatform.features.splash.ui.screens.splash

sealed interface SplashEvent {
    object UserNotAuthorized : SplashEvent
    object UserAuthorized : SplashEvent
}