package com.security.chat.multiplatform.common.core.time

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

internal class TimeProviderImpl : TimeProvider {

    @OptIn(ExperimentalTime::class)
    override fun now(): Instant = Clock.System.now()

}