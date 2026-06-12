package com.security.chat.multiplatform.features.authorize.domain.entity

public data class SignInStateInfo(
    val privateKey: String,
    val isSignInEnabled: Boolean,
    val isOnboardingPassed: Boolean,
)
