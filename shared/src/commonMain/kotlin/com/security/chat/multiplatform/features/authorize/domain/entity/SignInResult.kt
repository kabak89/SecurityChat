package com.security.chat.multiplatform.features.authorize.domain.entity

sealed interface SignInResult {

    object UserNotExists : SignInResult
    object Success : SignInResult
    object WrongPassword : SignInResult

}