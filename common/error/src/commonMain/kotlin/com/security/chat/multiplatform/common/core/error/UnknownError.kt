package com.security.chat.multiplatform.common.core.error

public data class UnknownError(
    val originalError: Throwable,
) : AppError()