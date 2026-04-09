package com.security.chat.multiplatform.features.authorize.domain.repo

import com.security.chat.multiplatform.features.authorize.domain.entity.SignUpResult

public interface SignUpRepo {
    public suspend fun signUp(username: String, password: String): SignUpResult
}