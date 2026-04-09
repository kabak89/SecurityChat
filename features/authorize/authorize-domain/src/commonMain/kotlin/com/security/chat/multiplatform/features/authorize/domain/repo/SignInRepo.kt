package com.security.chat.multiplatform.features.authorize.domain.repo

import com.security.chat.multiplatform.features.authorize.domain.entity.SignInResult

public interface SignInRepo {
    public suspend fun signIn(username: String, password: String): SignInResult
}