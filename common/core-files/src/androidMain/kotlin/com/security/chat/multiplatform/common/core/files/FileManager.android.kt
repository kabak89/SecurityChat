package com.security.chat.multiplatform.common.core.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import com.security.chat.multiplatform.common.core.files.error.TranscodeException
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID

internal class FileManagerAndroid(
    context: Context,
    private val dispatcherProvider: DispatcherProviderInterface,
) : FileManager {

    private val cacheDirectory = context.cacheDir
    private val dataDirectory = context.filesDir

    override suspend fun isImage(fileSource: FileSource): Boolean {
        return withContext(dispatcherProvider.IO) {
            val header = fileSource.contentResolver
                .openInputStream(fileSource.uri)
                ?.use { inputStream -> inputStream.readImageHeader() }
                ?: error("Cannot read selected file")

            header.hasImageSignature()
        }
    }

    override suspend fun isRenderable(path: String): Boolean {
        return withContext(dispatcherProvider.IO) {
            File(path).readImageHeader().hasRenderableImageSignature()
        }
    }

    override suspend fun transcodeToJpeg(path: String) {
        withContext(dispatcherProvider.IO) {
            val file = File(path)

            /** HEIF decoding was only added in Android 9, older versions have no decoder at all. */
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                throw TranscodeException("Cannot decode $path: requires Android 9 or newer")
            }

            val bitmap = try {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(file))
            } catch (error: IOException) {
                throw TranscodeException("Cannot decode $path", error)
            }

            val transcodedFile = File("$path$TRANSCODED_SUFFIX")
            try {
                transcodedFile.outputStream().use { outputStream ->
                    val isEncoded = bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        JPEG_COMPRESSION_QUALITY,
                        outputStream,
                    )
                    if (!isEncoded) throw TranscodeException("Cannot encode $path to JPEG")
                }
                if (!transcodedFile.renameTo(file)) {
                    throw TranscodeException("Cannot write transcoded image to $path")
                }
            } finally {
                bitmap.recycle()
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
                getOrCreateCacheDirectory(directoryName),
                UUID.randomUUID().toString(),
            )

            fileSource.contentResolver
                .openInputStream(fileSource.uri)
                ?.use { inputStream ->
                    FileOutputStream(destinationFile).use(inputStream::copyTo)
                }
                ?: error("Cannot read selected file")

            destinationFile.absolutePath
        }
    }

    override suspend fun getCacheDirectoryPath(name: String): String {
        return withContext(dispatcherProvider.IO) {
            getOrCreateCacheDirectory(name).absolutePath
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

            if (source.renameTo(destination)) return@withContext

            source.copyTo(destination, overwrite = true)
            source.delete()
        }
    }

    override suspend fun deleteFile(path: String) {
        withContext(dispatcherProvider.IO) {
            File(path).delete()
        }
    }

    override suspend fun fileExists(path: String): Boolean {
        return withContext(dispatcherProvider.IO) {
            File(path).isFile
        }
    }

    override suspend fun clearCacheDirectory(name: String) {
        withContext(dispatcherProvider.IO) {
            cacheDirectory.resolve(name).deleteRecursively()
        }
    }

    override suspend fun clearDataDirectory(name: String) {
        withContext(dispatcherProvider.IO) {
            dataDirectory.resolve(name).deleteRecursively()
        }
    }

    private fun getOrCreateCacheDirectory(name: String): File {
        val directory = cacheDirectory.resolve(name)
        check(directory.isDirectory || directory.mkdirs()) {
            "Cannot create cache directory: ${directory.absolutePath}"
        }
        return directory
    }
}

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

/** Kept next to the original file so that the rename stays on the same filesystem. */
private const val TRANSCODED_SUFFIX = ".jpeg"

private const val JPEG_COMPRESSION_QUALITY = 95
