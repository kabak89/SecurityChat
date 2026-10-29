package com.security.chat.multiplatform.testutil

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@OptIn(ExperimentalTestApi::class)
internal fun SkikoComposeUiTest.pauseForVideo(duration: Duration) {
    require(duration.isFinite() && duration >= Duration.ZERO) {
        "Pause duration must be finite and non-negative"
    }

    waitForIdle()
    val startedAt = TimeSource.Monotonic.markNow()
    waitUntil(timeoutMillis = (duration + 1.seconds).inWholeMilliseconds) {
        startedAt.elapsedNow() >= duration
    }
}
