package com.security.chat.multiplatform.features.authorize.domain.entity

public data class SignUpStateInfo(
    val username: String,
    val formFilled: Boolean,
    val isOnboardingPassed: Boolean,
)
