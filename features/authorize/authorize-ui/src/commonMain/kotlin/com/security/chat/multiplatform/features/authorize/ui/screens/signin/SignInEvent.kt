package com.security.chat.multiplatform.features.authorize.ui.screens.signin

internal sealed interface SignInEvent {
    object Authorized : SignInEvent
}