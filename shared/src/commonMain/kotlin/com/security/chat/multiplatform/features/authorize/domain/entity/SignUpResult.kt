package com.security.chat.multiplatform.features.authorize.domain.entity

sealed interface SignUpResult {

    object Success : SignUpResult
    object LoginAlreadyExists : SignUpResult

}