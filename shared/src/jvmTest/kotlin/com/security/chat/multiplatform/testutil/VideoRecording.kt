package com.security.chat.multiplatform.testutil

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.onRoot
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

internal const val VIDEO_FPS = 25
private const val FRAME_INTERVAL_MILLIS = 1_000L / VIDEO_FPS

@OptIn(ExperimentalTestApi::class)
internal suspend fun SkikoComposeUiTest.recordVideo(
    name: String,
    onVideoRecorded: (File) -> Unit = {},
    block: suspend SkikoComposeUiTest.() -> Unit,
) {
    require(name.matches(Regex("[a-zA-Z0-9_-]+"))) { "Invalid video name: $name" }
    val root = onRoot().fetchSemanticsNode()
    val output = File("build/reports/ui-tests/$name.mp4")

    FfmpegVideoRecorder(output = output, onVideoRecorded = onVideoRecorded).use { recorder ->
        fun captureFrame(): ByteArray =
            runOnUiThread {
                /** Capture without waiting for idle, which would skip intermediate frames. */
                val frame = captureToImage(root)
                Image.makeFromBitmap(frame.asSkiaBitmap()).use { image ->
                    checkNotNull(image.encodeToData(EncodedImageFormat.PNG)).use { data ->
                        data.bytes
                    }
                }
            }

        val timeline = VideoFrameTimeline(
            initialFrame = captureFrame(),
            writeFrame = recorder::writeFrame,
        )
        val startedAt = System.nanoTime()
        coroutineScope {
            /** Poll with Compose, but emit frames according to elapsed real time. */
            val captureJob = launch(
                StandardTestDispatcher(mainClock.scheduler) + mainClock.scheduler,
            ) {
                while (isActive) {
                    delay(FRAME_INTERVAL_MILLIS.milliseconds)
                    timeline.sample(
                        elapsedNanos = System.nanoTime() - startedAt,
                        captureFrame = ::captureFrame,
                    )
                }
            }
            var failure: Throwable? = null
            try {
                block()
            } catch (exception: Throwable) {
                failure = exception
                throw exception
            } finally {
                val elapsedNanos = System.nanoTime() - startedAt
                captureJob.cancel()
                try {
                    runOnUiThread { mainClock.scheduler.runCurrent() }
                    timeline.finish(
                        elapsedNanos = elapsedNanos,
                        captureFrame = ::captureFrame,
                    )
                } catch (exception: Throwable) {
                    val originalFailure = failure
                    if (originalFailure == null) {
                        throw exception
                    }
                    originalFailure.addSuppressed(exception)
                }
            }
        }
    }
}
