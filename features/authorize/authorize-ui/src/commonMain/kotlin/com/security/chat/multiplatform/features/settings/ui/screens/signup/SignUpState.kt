package com.security.chat.multiplatform.features.settings.ui.screens.signup

internal data class SignUpState(
    val username: String,
    val password: String,
    val passwordRepeat: String,
    val isLoading: Boolean,
    val nextButtonEnabled: Boolean,
)
