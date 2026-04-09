package com.security.chat.multiplatform.common.log

internal expect class Logger() {
    fun d(message: () -> String)
    fun e(error: Throwable, message: String?)
    fun e(message: String)
}
