package com.security.chat.multiplatform.common.core.network.entity

import kotlin.jvm.JvmInline

public data class Tokens(
    val accessToken: AccessToken,
    val refreshToken: RefreshToken,
)

@JvmInline
public value class AccessToken(public val value: String)

@JvmInline
public value class RefreshToken(public val value: String)
