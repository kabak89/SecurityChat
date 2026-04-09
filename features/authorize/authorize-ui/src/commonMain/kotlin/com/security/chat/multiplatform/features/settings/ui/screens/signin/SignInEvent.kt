package com.security.chat.multiplatform.features.settings.ui.screens.signin

internal sealed interface SignInEvent {
    object Authorized : SignInEvent
}