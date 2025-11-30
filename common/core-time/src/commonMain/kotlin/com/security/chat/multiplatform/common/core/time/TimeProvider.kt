package com.security.chat.multiplatform.common.core.time

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

public interface TimeProvider {

    @OptIn(ExperimentalTime::class)
    public fun now(): Instant

}