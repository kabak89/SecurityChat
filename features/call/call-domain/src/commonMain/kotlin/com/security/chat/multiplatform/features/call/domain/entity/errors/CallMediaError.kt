package com.security.chat.multiplatform.features.call.domain.entity.errors

import com.security.chat.multiplatform.common.core.error.AppError

public class CallMediaError(
    override val message: String,
    override val cause: Throwable? = null,
) : AppError()
