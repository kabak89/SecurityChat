package com.security.chat.multiplatform.common.crash.report

import kotlin.concurrent.Volatile

internal class CrashReporterIos : CrashReporter {

    override fun recordException(error: Throwable, message: String?) {
        val bridge = bridgeOrWarn() ?: return

        if (message != null) {
            bridge.log(message)
        }
        bridge.recordException(
            name = error::class.qualifiedName ?: UNKNOWN_EXCEPTION_NAME,
            reason = error.message ?: error.toString(),
            stackTrace = error.stackTraceToString().lines(),
        )
    }

    override fun setCustomKey(key: String, value: String) {
        bridgeOrWarn()?.setCustomKey(key, value)
    }

    override fun setUserId(userId: String?) {
        bridgeOrWarn()?.setUserId(userId)
    }

    private fun bridgeOrWarn(): IosCrashReporterBridge? {
        val bridge = IosCrashReporterBridgeHolder.bridge
        if (bridge == null && !isMissingBridgeReported) {
            isMissingBridgeReported = true
            /** Written to the console directly: `Log` feeds this reporter and would recurse. */
            println("CrashReporterIos: no Swift bridge registered, reports are dropped")
        }
        return bridge
    }

    private companion object {

        const val UNKNOWN_EXCEPTION_NAME = "Unknown Kotlin exception"

        @Volatile
        var isMissingBridgeReported: Boolean = false
    }
}
