package com.security.chat.multiplatform.common.log

internal actual class Logger {

    actual fun d(message: () -> String) {
        if (!BuildKonfig.ENABLE_LOGS) return
        println(message())
    }

    actual fun e(error: Throwable, message: String?) {
        if (!BuildKonfig.ENABLE_LOGS) return
        println(error)
        if (message != null) {
            println(message)
        }
    }

    actual fun e(message: String) {
        if (!BuildKonfig.ENABLE_LOGS) return
        println(message)
    }
}
