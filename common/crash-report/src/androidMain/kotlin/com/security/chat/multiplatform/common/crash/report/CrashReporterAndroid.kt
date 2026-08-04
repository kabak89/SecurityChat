package com.security.chat.multiplatform.common.crash.report

import com.google.firebase.crashlytics.FirebaseCrashlytics

internal class CrashReporterAndroid : CrashReporter {

    /** Resolved on every call: Firebase is initialized by its own content provider. */
    private val crashlytics: FirebaseCrashlytics
        get() = FirebaseCrashlytics.getInstance()

    override fun recordException(error: Throwable, message: String?) {
        if (message != null) {
            crashlytics.log(message)
        }
        crashlytics.recordException(error)
    }

    override fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    override fun setUserId(userId: String?) {
        crashlytics.setUserId(userId.orEmpty())
    }
}
