package com.security.chat.multiplatform.features.settings.ui.screens.signup

internal sealed interface SignUpEvent {
    object SuccessSignUp : SignUpEvent
}