package com.security.chat.multiplatform.common.core.network

import com.security.chat.multiplatform.common.core.error.AppError

public expect fun resolveError(throwable: Throwable): AppError