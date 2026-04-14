package com.security.chat.multiplatform.common.core.error

public data class NetworkError(
    val statusCode: Int,
) : AppError()