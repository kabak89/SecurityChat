package com.security.chat.multiplatform.common.crash.report

import com.security.chat.multiplatform.common.log.Log

internal actual val platformCrashReporter: CrashReporter = CrashReporterJvm()

internal actual fun installPlatformCrashHandler(reporter: CrashReporter) {
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    Thread.setDefaultUncaughtExceptionHandler { thread, error ->
        Log.e(error, "uncaught exception in thread ${thread.name}")
        defaultHandler?.uncaughtException(thread, error)
    }
}
