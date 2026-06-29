package com.security.chat.multiplatform.common.core.network

import com.security.chat.multiplatform.common.core.error.AppError
import com.security.chat.multiplatform.common.core.error.UnknownError
import com.security.chat.multiplatform.common.core.error.WrongResponseError
import io.ktor.serialization.JsonConvertException

internal fun resolveError(throwable: Throwable): AppError {
    return when (throwable) {
        is JsonConvertException -> WrongResponseError(throwable)
        else -> UnknownError(originalError = throwable)
    }
}