package com.security.chat.multiplatform.common.log

public object Log {

    @PublishedApi
    internal var logger: Logger = Logger()

    public fun d(
        message: () -> String,
    ) {
        logger.d(message = message)
    }

    public fun e(
        error: Throwable,
        message: String? = null,
    ) {
        logger.e(
            error = error,
            message = message,
        )
    }

    public fun e(
        message: String,
    ) {
        logger.e(
            message = message,
        )
    }
}
