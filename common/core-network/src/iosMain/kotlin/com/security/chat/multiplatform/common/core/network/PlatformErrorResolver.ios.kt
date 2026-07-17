package com.security.chat.multiplatform.common.core.network

import com.security.chat.multiplatform.common.core.error.AppError
import com.security.chat.multiplatform.common.core.error.ConnectionError
import io.ktor.client.engine.darwin.DarwinHttpRequestException

public actual fun resolvePlatformError(throwable: Throwable): AppError? {
    return when (throwable) {
        is DarwinHttpRequestException -> ConnectionError()
        else -> null
    }
}