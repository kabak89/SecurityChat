package com.security.chat.multiplatform.testutil

internal class VideoFrameTimeline(
    initialFrame: ByteArray,
    private val writeFrame: (ByteArray) -> Unit,
) {
    private var pendingFrame = initialFrame
    private var frameIndex = 0L

    fun sample(elapsedNanos: Long, captureFrame: () -> ByteArray) {
        val nextFrameIndex = elapsedNanos / FRAME_INTERVAL_NANOS
        if (nextFrameIndex <= frameIndex) return

        val nextFrame = captureFrame()
        writeUntil(nextFrameIndex)
        pendingFrame = nextFrame
    }

    fun finish(elapsedNanos: Long, captureFrame: () -> ByteArray) {
        val finalFrame = captureFrame()
        writeUntil(elapsedNanos / FRAME_INTERVAL_NANOS)
        /** Keep the current slot pending so the final state fits without an artificial hold. */
        writeFrame(finalFrame)
    }

    private fun writeUntil(nextFrameIndex: Long) {
        while (frameIndex < nextFrameIndex) {
            writeFrame(pendingFrame)
            frameIndex++
        }
    }
}

private const val FRAME_INTERVAL_NANOS = 1_000_000_000L / VIDEO_FPS
