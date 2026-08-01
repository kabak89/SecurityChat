package com.security.chat.multiplatform.common.core.files

import com.security.chat.multiplatform.common.core.files.error.TranscodeException
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class FileManagerJvm(
    private val dispatcherProvider: DispatcherProviderInterface,
) : FileManager {

    private val cacheDirectory = File(System.getProperty("user.home"), ".SecurityChat/cache")
    private val dataDirectory = File(System.getProperty("user.home"), ".SecurityChat/data")

    override suspend fun isImage(fileSource: FileSource): Boolean {
        return withContext(dispatcherProvider.IO) {
            fileSource.file.readImageHeader().hasImageSignature()
        }
    }

    override suspend fun isRenderable(path: String): Boolean {
        return withContext(dispatcherProvider.IO) {
            File(path).readImageHeader().hasRenderableImageSignature()
        }
    }

    override suspend fun transcodeToJpeg(path: String) {
        withContext(dispatcherProvider.IO) {
            if (!isMacOs) {
                throw TranscodeException("Cannot transcode $path: unsupported desktop platform")
            }

            val file = File(path)
            val transcodedFile = File("$path$TRANSCODED_SUFFIX")
            try {
                transcodeWithSips(source = file, destination = transcodedFile)
                if (!transcodedFile.renameTo(file)) {
                    throw TranscodeException("Cannot write transcoded image to $path")
                }
            } finally {
                /** Nothing is left at that path once the rename succeeded. */
                transcodedFile.delete()
            }
        }
    }

    override suspend fun copyToCache(
        fileSource: FileSource,
        directoryName: String,
    ): String {
        return withContext(dispatcherProvider.IO) {
            val destinationFile = File(
                getOrCreateDirectory(directoryName),
                UUID.randomUUID().toString(),
            )

            fileSource.file.copyTo(destinationFile)

            destinationFile.absolutePath
        }
    }

    override suspend fun getDirectoryPath(name: String): String {
        return withContext(dispatcherProvider.IO) {
            getOrCreateDirectory(name).absolutePath
        }
    }

    override suspend fun getDataDirectoryPath(name: String): String {
        return withContext(dispatcherProvider.IO) {
            val directory = dataDirectory.resolve(name)
            check(directory.isDirectory || directory.mkdirs()) {
                "Cannot create data directory: ${directory.absolutePath}"
            }
            directory.absolutePath
        }
    }

    override suspend fun moveFile(sourcePath: String, destinationPath: String) {
        withContext(dispatcherProvider.IO) {
            val source = File(sourcePath)
            val destination = File(destinationPath)
            destination.parentFile?.mkdirs()
            source.copyTo(destination, overwrite = true)
            source.delete()
        }
    }

    override suspend fun deleteFile(path: String) {
        withContext(dispatcherProvider.IO) {
            File(path).delete()
        }
    }

    override suspend fun clearDirectory(name: String) {
        withContext(dispatcherProvider.IO) {
            val directory = cacheDirectory.resolve(name)
            directory.deleteRecursively()
            check(directory.mkdirs()) {
                "Cannot create cache directory: ${directory.absolutePath}"
            }
        }
    }

    private fun getOrCreateDirectory(name: String): File {
        val directory = cacheDirectory.resolve(name)
        check(directory.isDirectory || directory.mkdirs()) {
            "Cannot create cache directory: ${directory.absolutePath}"
        }
        return directory
    }

    /**
     * `sips` ships with macOS and decodes through the same system codecs as Preview, so it needs
     * neither a bundled library nor a matching file extension. Desktop has no other option: Skia
     * and ImageIO both lack a HEIF decoder, and the rest pull a native library into the build.
     */
    private fun transcodeWithSips(source: File, destination: File) {
        val process = ProcessBuilder(
            SIPS_PATH,
            "--setProperty", "format", "jpeg",
            "--setProperty", "formatOptions", JPEG_COMPRESSION_QUALITY.toString(),
            source.absolutePath,
            "--out", destination.absolutePath,
        )
            .redirectErrorStream(true)
            .start()

        val isFinished = process.waitFor(SIPS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!isFinished) {
            process.destroyForcibly()
            throw TranscodeException("Timed out transcoding ${source.absolutePath}")
        }

        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()

        /**
         * An unreadable source is only reported as a warning and skipped, so a zero exit code
         * alone does not mean the image was written.
         */
        if (process.exitValue() != 0 || !destination.isFile) {
            throw TranscodeException("Cannot transcode ${source.absolutePath}: $output")
        }
    }
}

private val isMacOs: Boolean
    get() = System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)

private fun File.readImageHeader(): ByteArray {
    return inputStream().use { inputStream -> inputStream.readImageHeader() }
}

private fun InputStream.readImageHeader(): ByteArray {
    val header = ByteArray(IMAGE_HEADER_SIZE)
    var offset = 0
    while (offset < IMAGE_HEADER_SIZE) {
        val readCount = read(header, offset, IMAGE_HEADER_SIZE - offset)
        if (readCount < 0) break
        offset += readCount
    }
    return header.copyOf(offset)
}

/** Absolute path so that the tool cannot be shadowed through `PATH`. */
private const val SIPS_PATH = "/usr/bin/sips"

/** Kept next to the original file so that the rename stays on the same filesystem. */
private const val TRANSCODED_SUFFIX = ".jpg"

private const val JPEG_COMPRESSION_QUALITY = 95
private const val SIPS_TIMEOUT_SECONDS = 30L
