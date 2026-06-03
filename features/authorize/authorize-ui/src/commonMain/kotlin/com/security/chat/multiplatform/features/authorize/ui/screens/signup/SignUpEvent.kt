package com.security.chat.multiplatform.features.authorize.ui.screens.signup

internal sealed interface SignUpEvent {
    object SuccessSignUp : SignUpEvent
}