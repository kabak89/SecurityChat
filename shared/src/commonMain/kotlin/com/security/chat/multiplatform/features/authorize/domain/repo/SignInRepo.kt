package com.security.chat.multiplatform.features.authorize.domain.repo

import com.security.chat.multiplatform.features.authorize.domain.entity.AuthResult

interface SignInRepo {

    suspend fun authorize(username: String, password: String): AuthResult

}