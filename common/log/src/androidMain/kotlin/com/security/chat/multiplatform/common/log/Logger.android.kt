package com.security.chat.multiplatform.common.log

import timber.log.Timber

internal actual class Logger {

    init {
        if (BuildKonfig.ENABLE_LOGS) {
            Timber.plant(LinkingDebugTree())
        }
    }

    actual fun d(message: () -> String) {
        if (!BuildKonfig.ENABLE_LOGS) return
        Timber.d(message())
    }

    actual fun e(error: Throwable, message: String?) {
        if (!BuildKonfig.ENABLE_LOGS) return
        Timber.e(error, message)
    }

    actual fun e(message: String) {
        if (!BuildKonfig.ENABLE_LOGS) return
        Timber.e(message)
    }
}