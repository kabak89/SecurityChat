package com.security.chat.multiplatform.common.core.files

import com.security.chat.multiplatform.common.core.files.error.TranscodeException
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.log.Log
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSItemProvider
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.closeFile
import platform.Foundation.fileHandleForReadingAtPath
import platform.Foundation.readDataOfLength
import platform.Foundation.writeToFile
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class FileManagerIos(
    private val dispatcherProvider: DispatcherProviderInterface,
) : FileManager {

    /**
     * The item provider exposes no bytes until a representation is exported, and exporting it
     * twice would double the cost of picking a photo, so the declared type is asked instead.
     */
    override suspend fun isImage(fileSource: FileSource): Boolean {
        return fileSource.itemProvider.hasItemConformingToTypeIdentifier(IMAGE_TYPE_IDENTIFIER)
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun isRenderable(path: String): Boolean {
        return withContext(dispatcherProvider.IO) {
            readImageHeader(path).hasRenderableImageSignature()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun transcodeToJpeg(path: String) {
        withContext(dispatcherProvider.IO) {
            val image = UIImage.imageWithContentsOfFile(path = path)
                ?: throw TranscodeException("Cannot decode $path")

            val size = image.size
            val uprightImage = size.useContents {
                UIGraphicsBeginImageContextWithOptions(
                    size = size,
                    opaque = true,
                    scale = image.scale,
                )
                image.drawInRect(
                    CGRectMake(x = 0.0, y = 0.0, width = width, height = height),
                )
                val redrawnImage = UIGraphicsGetImageFromCurrentImageContext()
                UIGraphicsEndImageContext()
                redrawnImage
            } ?: throw TranscodeException("Cannot redraw $path")

            val jpegData = UIImageJPEGRepresentation(uprightImage, JPEG_COMPRESSION_QUALITY)
                ?: throw TranscodeException("Cannot encode $path to JPEG")

            if (!jpegData.writeToFile(path = path, atomically = true)) {
                throw TranscodeException("Cannot write transcoded image to $path")
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun copyToCache(
        fileSource: FileSource,
        directoryName: String,
    ): String {
        return withContext(dispatcherProvider.IO) {
            val directoryPath = getCacheDirectoryPath(directoryName)
            val destinationPath = "$directoryPath/${NSUUID.UUID().UUIDString}"
            val typeIdentifiers = fileSource.itemProvider.registeredTypeIdentifiers
                .filterIsInstance<String>()

            /**
             * The picker exports the asset in the requested representation, so asking for a
             * decodable one lets the system transcode instead of doing it here.
             */
            val typeIdentifier = typeIdentifiers
                .firstOrNull { it in DECODABLE_TYPE_IDENTIFIERS }
                ?: typeIdentifiers.firstOrNull()
                ?: error("Selected file has no supported data representation")

            fileSource.itemProvider.copyTo(destinationPath, typeIdentifier)

            destinationPath
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun getCacheDirectoryPath(name: String): String {
        return withContext(dispatcherProvider.IO) {
            val directoryPath = "$cacheDirectoryPath/$name"
            createDirectory(directoryPath)
            directoryPath
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun getDataDirectoryPath(name: String): String {
        return withContext(dispatcherProvider.IO) {
            val directoryPath = "$dataDirectoryPath/$name"
            createDirectory(directoryPath)
            directoryPath
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun moveFile(sourcePath: String, destinationPath: String) {
        withContext(dispatcherProvider.IO) {
            NSFileManager.defaultManager.removeItemAtPath(
                path = destinationPath,
                error = null,
            )
            memScoped {
                val errorRef = alloc<ObjCObjectVar<NSError?>>()
                val isMoved = NSFileManager.defaultManager.moveItemAtPath(
                    srcPath = sourcePath,
                    toPath = destinationPath,
                    error = errorRef.ptr,
                )
                if (!isMoved) {
                    Log.e(
                        "Cannot move $sourcePath to $destinationPath: " +
                                (errorRef.value?.localizedDescription ?: "unknown error"),
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun deleteFile(path: String) {
        withContext(dispatcherProvider.IO) {
            NSFileManager.defaultManager.removeItemAtPath(
                path = path,
                error = null,
            )
        }
    }

    /** The single-argument overload also reports directories, which are not files. */
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun fileExists(path: String): Boolean {
        return withContext(dispatcherProvider.IO) {
            memScoped {
                val isDirectory = alloc<BooleanVar>()
                val exists = NSFileManager.defaultManager.fileExistsAtPath(
                    path = path,
                    isDirectory = isDirectory.ptr,
                )
                exists && !isDirectory.value
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun clearCacheDirectory(name: String) {
        withContext(dispatcherProvider.IO) {
            NSFileManager.defaultManager.removeItemAtPath(
                path = "$cacheDirectoryPath/$name",
                error = null,
            )
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun clearDataDirectory(name: String) {
        withContext(dispatcherProvider.IO) {
            NSFileManager.defaultManager.removeItemAtPath(
                path = "$dataDirectoryPath/$name",
                error = null,
            )
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private val cacheDirectoryPath: String
        get() {
            val cacheDirectory = requireNotNull(
                NSFileManager.defaultManager.URLForDirectory(
                    directory = NSCachesDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = true,
                    error = null,
                ),
            )
            return requireNotNull(cacheDirectory.path)
        }

    @OptIn(ExperimentalForeignApi::class)
    private val dataDirectoryPath: String
        get() {
            val dataDirectory = requireNotNull(
                NSFileManager.defaultManager.URLForDirectory(
                    directory = NSDocumentDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = true,
                    error = null,
                ),
            )
            return requireNotNull(dataDirectory.path)
        }

    @OptIn(ExperimentalForeignApi::class)
    private fun createDirectory(path: String) {
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = path,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun readImageHeader(path: String): ByteArray {
        val fileHandle = NSFileHandle.fileHandleForReadingAtPath(path) ?: return ByteArray(0)
        val header = try {
            fileHandle.readDataOfLength(IMAGE_HEADER_SIZE.toULong())
        } finally {
            fileHandle.closeFile()
        }
        return header.toByteArray()
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun NSItemProvider.copyTo(
        destinationPath: String,
        typeIdentifier: String,
    ) {
        suspendCancellableCoroutine { continuation ->
            loadFileRepresentationForTypeIdentifier(typeIdentifier) { url, error ->
                if (url == null) {
                    continuation.resumeWithException(
                        IllegalStateException(
                            error?.localizedDescription ?: "Cannot read selected file",
                        ),
                    )
                    return@loadFileRepresentationForTypeIdentifier
                }
                if (
                    !NSFileManager.defaultManager.copyItemAtURL(
                        srcURL = url,
                        toURL = NSURL.fileURLWithPath(destinationPath),
                        error = null,
                    )
                ) {
                    continuation.resumeWithException(
                        IllegalStateException("Cannot write selected file to cache"),
                    )
                    return@loadFileRepresentationForTypeIdentifier
                }
                continuation.resume(Unit)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    return bytes?.readBytes(size) ?: ByteArray(0)
}

/** Uniform type identifiers Skia is able to decode, preferred over whatever the picker offers. */
private val DECODABLE_TYPE_IDENTIFIERS = setOf("public.jpeg", "public.png")

/** Base uniform type identifier every image format conforms to. */
private const val IMAGE_TYPE_IDENTIFIER = "public.image"

private const val JPEG_COMPRESSION_QUALITY = 0.95
