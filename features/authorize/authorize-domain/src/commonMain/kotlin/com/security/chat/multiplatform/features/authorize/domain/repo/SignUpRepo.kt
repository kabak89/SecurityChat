package com.security.chat.multiplatform.features.authorize.domain.repo

public interface SignUpRepo {
    public suspend fun signUp(username: String)
    public suspend fun isOnboardingPassed(): Boolean
}