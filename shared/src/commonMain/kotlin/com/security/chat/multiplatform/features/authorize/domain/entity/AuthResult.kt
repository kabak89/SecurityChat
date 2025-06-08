package com.security.chat.multiplatform.features.authorize.domain.entity

sealed interface AuthResult {

    object UserNotExists : AuthResult
    object Success : AuthResult
    object WrongPassword : AuthResult

}