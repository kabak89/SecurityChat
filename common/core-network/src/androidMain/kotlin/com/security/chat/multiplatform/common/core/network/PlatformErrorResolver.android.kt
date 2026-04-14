package com.security.chat.multiplatform.common.core.network

import com.security.chat.multiplatform.common.core.error.AppError
import com.security.chat.multiplatform.common.core.error.ConnectionError
import com.security.chat.multiplatform.common.core.error.UnknownError
import java.net.ConnectException

public actual fun resolveError(throwable: Throwable): AppError {
    return when (throwable) {
        is ConnectException -> ConnectionError()
        else -> UnknownError()
    }
}