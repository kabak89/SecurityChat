package com.security.chat.multiplatform.common.log

public object Log {

    private val logger: Logger = Logger()

    private val enableLogs: Boolean = BuildKonfig.ENABLE_LOGS

    public fun d(message: () -> String) {
        if (!enableLogs) return
        logger.d(message())
    }

    public fun e(
        error: Throwable,
        message: String? = null,
    ) {
        if (!enableLogs) return
        logger.e(
            error = error,
            message = message,
        )
    }

    public fun e(message: String) {
        if (!enableLogs) return
        logger.e(
            message = message,
        )
    }
}
