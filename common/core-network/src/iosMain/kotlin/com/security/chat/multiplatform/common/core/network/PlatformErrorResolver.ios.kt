package com.security.chat.multiplatform.common.core.network

import com.security.chat.multiplatform.common.core.error.AppError
import com.security.chat.multiplatform.common.core.error.UnknownError

public actual fun resolveError(throwable: Throwable): AppError {
    return UnknownError()
}