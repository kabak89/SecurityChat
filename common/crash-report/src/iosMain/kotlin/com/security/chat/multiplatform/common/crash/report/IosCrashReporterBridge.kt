package com.security.chat.multiplatform.common.crash.report

import kotlin.concurrent.Volatile

/**
 * Kotlin side of the Swift bridge: the Crashlytics SDK lives in the iOS app, so reports are handed
 * over to an implementation registered from Swift at startup. Exceptions are passed as plain
 * strings because Crashlytics builds its report from an exception name, a reason and stack frames.
 */
public interface IosCrashReporterBridge {

    public fun recordException(name: String, reason: String, stackTrace: List<String>)

    public fun log(message: String)

    public fun setCustomKey(key: String, value: String)

    public fun setUserId(userId: String?)
}

/**
 * Must be called from Swift before [initCrashReporting] so that startup errors are reported too.
 */
public fun setIosCrashReporterBridge(bridge: IosCrashReporterBridge?) {
    IosCrashReporterBridgeHolder.bridge = bridge
}

internal object IosCrashReporterBridgeHolder {

    @Volatile
    var bridge: IosCrashReporterBridge? = null
}
