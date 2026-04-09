package com.security.chat.multiplatform.features.authorize.domain.entity

public data class SignUpStateInfo(
    val username: String,
    val password: String,
    val passwordRepeat: String,
    val formFilled: Boolean,
)
