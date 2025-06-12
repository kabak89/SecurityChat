package com.security.chat.multiplatform.features.authorize.ui.screens.signup

data class SignUpState(
    val username: String,
    val password: String,
    val passwordRepeat: String,
    val isLoading: Boolean,
    val nextButtonEnabled: Boolean,
)
