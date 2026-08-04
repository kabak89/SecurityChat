package com.security.chat.multiplatform.common.crash.report

/**
 * Crashlytics has no JVM SDK, so on desktop reporting stays local: fatal errors are written to the
 * log by the default uncaught exception handler and non-fatals are dropped here.
 *
 * Must not call `Log` itself: `Log` feeds this reporter and the call would come back here.
 */
internal class CrashReporterJvm : CrashReporter {

    override fun recordException(error: Throwable, message: String?): Unit = Unit
    override fun setCustomKey(key: String, value: String): Unit = Unit
    override fun setUserId(userId: String?): Unit = Unit
}
