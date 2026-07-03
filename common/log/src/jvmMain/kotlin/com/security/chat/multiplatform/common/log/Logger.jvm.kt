package com.security.chat.multiplatform.common.log

internal actual class Logger {

    actual fun d(message: String) {
        println(message)
    }

    actual fun e(error: Throwable, message: String?) {
        println(error)
        if (message != null) {
            println(message)
        }
    }

    actual fun e(message: String) {
        println(message)
    }
}
