package com.security.chat.multiplatform.common.crash.report

internal actual val platformCrashReporter: CrashReporter = CrashReporterIos()

/**
 * Unhandled Kotlin exceptions are picked up by NSExceptionKt, which is registered from Swift
 * together with the Crashlytics reporter.
 */
internal actual fun installPlatformCrashHandler(reporter: CrashReporter): Unit = Unit
