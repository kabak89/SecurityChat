package com.security.chat.multiplatform.features.authorize.domain.repo

public interface SignInRepo {
    public suspend fun signIn(username: String, password: String)
    public suspend fun isOnboardingPassed(): Boolean
}