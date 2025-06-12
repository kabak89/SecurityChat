package com.security.chat.multiplatform.features.authorize.domain.repo

import com.security.chat.multiplatform.features.authorize.domain.entity.SignInResult

interface SignInRepo {

    suspend fun signIn(username: String, password: String): SignInResult

}