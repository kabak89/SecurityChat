package com.security.chat.multiplatform.common.log

import kotlin.concurrent.Volatile

public object Log {

    private val logger: Logger = Logger()

    private val enableLogs: Boolean = BuildKonfig.ENABLE_LOGS

    @Volatile
    private var sinks: List<LogSink> = emptyList()

    /**
     * Expected to be called during app startup, before errors can be reported from other threads.
     */
    public fun addSink(sink: LogSink) {
        sinks = sinks + sink
    }

    public fun d(message: () -> String) {
        if (!enableLogs) return
        logger.d(message())
    }

    public fun e(
        error: Throwable,
        message: String? = null,
    ) {
        notifySinks(
            error = error,
            message = message,
        )
        if (!enableLogs) return
        logger.e(
            error = error,
            message = message,
        )
    }

    public fun e(message: String) {
        notifySinks(
            error = null,
            message = message,
        )
        if (!enableLogs) return
        logger.e(
            message = message,
        )
    }

    private fun notifySinks(
        error: Throwable?,
        message: String?,
    ) {
        sinks.forEach { sink ->
            sink.onError(
                error = error,
                message = message,
            )
        }
    }
}
