package com.security.chat.multiplatform.features.authorize.domain.repo

public interface SignInRepo {
    public suspend fun signIn(privateKey: String)
    public suspend fun isOnboardingPassed(): Boolean
}