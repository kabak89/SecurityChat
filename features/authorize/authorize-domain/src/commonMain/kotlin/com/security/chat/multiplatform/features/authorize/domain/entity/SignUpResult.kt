package com.security.chat.multiplatform.features.authorize.domain.entity

public sealed interface SignUpResult {
    public object Success : SignUpResult
    public object LoginAlreadyExists : SignUpResult
}