package com.security.chat.multiplatform.features.call.domain.entity.errors

import com.security.chat.multiplatform.common.core.error.AppError

public class CallSignalingError(
    public val code: String,
    override val message: String = code,
    override val cause: Throwable? = null,
) : AppError()
