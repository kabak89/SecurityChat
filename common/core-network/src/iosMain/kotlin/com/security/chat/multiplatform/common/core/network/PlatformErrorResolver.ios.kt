package com.security.chat.multiplatform.common.core.network

import com.security.chat.multiplatform.common.core.error.AppError

public actual fun resolvePlatformError(throwable: Throwable): AppError? {
    return null
}