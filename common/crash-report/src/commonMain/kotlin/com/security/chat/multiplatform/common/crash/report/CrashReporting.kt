package com.security.chat.multiplatform.common.crash.report

import com.security.chat.multiplatform.common.log.Log

internal expect val platformCrashReporter: CrashReporter

internal expect fun installPlatformCrashHandler(reporter: CrashReporter)

/**
 * Must be called once at app startup, before the rest of the initialization, so that failures on
 * the startup path are reported too.
 */
public fun initCrashReporting() {
    val reporter = platformCrashReporter

    installPlatformCrashHandler(reporter)

    Log.addSink { error, message ->
        when {
            error != null -> reporter.recordException(error, message)
            message != null -> reporter.recordException(Exception(message))
        }
    }
}
