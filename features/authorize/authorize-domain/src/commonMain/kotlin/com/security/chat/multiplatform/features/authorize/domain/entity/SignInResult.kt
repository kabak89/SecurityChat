package com.security.chat.multiplatform.features.authorize.domain.entity

public sealed interface SignInResult {
    public object UserNotExists : SignInResult
    public object Success : SignInResult
    public object WrongPassword : SignInResult
}