package com.security.chat.multiplatform.common.log

import timber.log.Timber

internal actual class Logger {

    init {
        if (BuildKonfig.IS_DEBUG) {
            Timber.plant(LinkingDebugTree())
        }
    }

    actual fun d(message: () -> String) {
        Timber.d(message())
    }

    actual fun e(error: Throwable, message: String?) {
        Timber.e(error, message)
    }

    actual fun e(message: String) {
        Timber.e(message)
    }
}