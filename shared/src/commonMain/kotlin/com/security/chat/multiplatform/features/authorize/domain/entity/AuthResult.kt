package com.security.chat.multiplatform.features.authorize.domain.entity

interface AuthResult {

    object UserNotExists : AuthResult
    object Success : AuthResult
    object WrongPassword : AuthResult

}