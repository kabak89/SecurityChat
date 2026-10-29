package com.security.chat.multiplatform.testutil

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class VideoFrameTimelineTest {

    @Test
    fun `frequent polls do not lengthen a short video and final state is kept`() {
        val frames = mutableListOf<ByteArray>()
        val timeline = VideoFrameTimeline(
            initialFrame = byteArrayOf(0),
            writeFrame = { frames += it },
        )

        repeat(1_000) {
            timeline.sample(elapsedNanos = 10.milliseconds.inWholeNanoseconds) {
                error("An extra frame must not be captured within the same time slot")
            }
        }
        timeline.finish(elapsedNanos = 20.milliseconds.inWholeNanoseconds) { byteArrayOf(1) }

        assertEquals(listOf<Byte>(1), frames.map { it.single() })
    }

    @Test
    fun `a five second gap is filled with previous frames and the final state`() {
        val frames = mutableListOf<ByteArray>()
        val timeline = VideoFrameTimeline(
            initialFrame = byteArrayOf(0),
            writeFrame = { frames += it },
        )

        timeline.finish(elapsedNanos = 5.seconds.inWholeNanoseconds) { byteArrayOf(1) }

        assertEquals(126, frames.size)
        assertEquals(List<Byte>(125) { 0 }, frames.dropLast(1).map { it.single() })
        assertEquals(1.toByte(), frames.last().single())
    }

    @Test
    fun `irregular samples preserve real time spacing between screen changes`() {
        val frames = mutableListOf<ByteArray>()
        val timeline = VideoFrameTimeline(
            initialFrame = byteArrayOf(0),
            writeFrame = { frames += it },
        )

        timeline.sample(elapsedNanos = 80.milliseconds.inWholeNanoseconds) { byteArrayOf(1) }
        timeline.finish(elapsedNanos = 180.milliseconds.inWholeNanoseconds) { byteArrayOf(2) }

        assertEquals(listOf<Byte>(0, 0, 1, 1, 2), frames.map { it.single() })
    }
}
