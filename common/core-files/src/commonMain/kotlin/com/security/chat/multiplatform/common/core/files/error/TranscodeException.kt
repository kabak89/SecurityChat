package com.security.chat.multiplatform.common.core.files.error

/** Thrown when the image cannot be rewritten as JPEG on the current platform. */
public class TranscodeException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)