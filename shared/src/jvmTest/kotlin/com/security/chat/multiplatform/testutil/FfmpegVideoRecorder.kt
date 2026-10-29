package com.security.chat.multiplatform.testutil

import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class FfmpegVideoRecorder(
    private val output: File,
    private val operationTimeout: Duration = 20.seconds,
    startProcess: (ProcessBuilder) -> Process = { it.start() },
    private val onVideoRecorded: (File) -> Unit = {},
) : AutoCloseable {
    private val log = File(output.parentFile, "${output.nameWithoutExtension}-ffmpeg.log")
    private val process: Process
    private val input: OutputStream
    private val writer = Executors.newSingleThreadExecutor { task ->
        Thread(task, "ui-test-ffmpeg-writer").apply { isDaemon = true }
    }
    private var failure: Throwable? = null
    private var closed = false

    init {
        require(operationTimeout.isPositive() && operationTimeout.isFinite())
        output.parentFile.mkdirs()
        val executable = System.getenv("FFMPEG_PATH") ?: "ffmpeg"
        process = try {
            startProcess(
                ProcessBuilder(
                    executable,
                    "-hide_banner",
                    "-loglevel", "error",
                    "-y",
                    "-f", "image2pipe",
                    "-framerate", VIDEO_FPS.toString(),
                    "-c:v", "png",
                    "-i", "pipe:0",
                    "-an",
                    "-c:v", "libx264",
                    "-preset", "veryfast",
                    "-crf", "23",
                    "-vf", "pad=ceil(iw/2)*2:ceil(ih/2)*2",
                    "-pix_fmt", "yuv420p",
                    "-movflags", "+faststart",
                    output.absolutePath,
                )
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(log),
            )
        } catch (exception: IOException) {
            throw IllegalStateException(
                "Cannot start FFmpeg. Install ffmpeg on PATH or set FFMPEG_PATH to its executable.",
                exception,
            )
        }
        input = process.outputStream.buffered()
    }

    fun writeFrame(png: ByteArray) {
        check(!closed) { "Video recorder is closed" }
        failure?.let { throw IllegalStateException("FFmpeg has already stopped", it) }
        runOperation("writing a frame") { input.write(png) }
    }

    override fun close() {
        if (closed) return
        closed = true
        try {
            if (failure == null) {
                runOperation("finalizing the video") {
                    input.close()
                    check(process.waitFor() == 0) {
                        "FFmpeg exited with code ${process.exitValue()}. See ${log.absolutePath}"
                    }
                }
            }
        } finally {
            try {
                if (process.isAlive) process.destroyForcibly()
            } finally {
                writer.shutdownNow()
            }
        }
        if (failure == null) {
            onVideoRecorded(output)
        }
    }

    private fun runOperation(description: String, operation: () -> Unit) {
        val pending = writer.submit(Callable { operation() })
        try {
            pending.get(
                operationTimeout.inWholeMilliseconds.coerceAtLeast(1),
                TimeUnit.MILLISECONDS,
            )
        } catch (exception: TimeoutException) {
            abort(
                exception = IllegalStateException(
                    "FFmpeg timed out while $description after $operationTimeout. " +
                            "See ${log.absolutePath}",
                    exception,
                ),
                pending = pending,
            )
        } catch (exception: ExecutionException) {
            abort(exception.cause ?: exception, pending)
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            abort(exception, pending)
        }
    }

    private fun abort(exception: Throwable, pending: Future<*>): Nothing {
        failure = exception
        try {
            if (process.isAlive) process.destroyForcibly()
        } catch (cleanupFailure: Throwable) {
            exception.addSuppressed(cleanupFailure)
        } finally {
            pending.cancel(true)
            writer.shutdownNow()
        }
        throw exception
    }
}
