package com.security.chat.multiplatform.common.crash.report

internal actual val platformCrashReporter: CrashReporter = CrashReporterAndroid()

/** Crashlytics installs its own uncaught exception handler while Firebase starts up. */
internal actual fun installPlatformCrashHandler(reporter: CrashReporter): Unit = Unit
