package com.security.chat.multiplatform.common.crash.report

public interface CrashReporter {

    public fun recordException(error: Throwable, message: String? = null)
    public fun setCustomKey(key: String, value: String)
    public fun setUserId(userId: String?)
}
