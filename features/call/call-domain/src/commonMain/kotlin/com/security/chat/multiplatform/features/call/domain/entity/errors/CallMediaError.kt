package com.security.chat.multiplatform.features.call.domain.entity.errors

import com.security.chat.multiplatform.common.core.error.AppError

public sealed class CallMediaError : AppError() {

    public class MicrophonePermission : CallMediaError() {
        override val message: String = "Microphone permission is required"
    }

    public class Audio(
        override val message: String,
        override val cause: Throwable? = null,
    ) : CallMediaError()

    public class Connection(
        override val message: String,
        override val cause: Throwable? = null,
    ) : CallMediaError()
}
