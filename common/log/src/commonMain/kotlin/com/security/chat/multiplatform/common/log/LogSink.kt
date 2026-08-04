package com.security.chat.multiplatform.common.log

public fun interface LogSink {

    public fun onError(error: Throwable?, message: String?)
}
