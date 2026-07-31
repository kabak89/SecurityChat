package com.security.chat.multiplatform.common.core.files

import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.log.Log
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSItemProvider
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
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

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun copyToCache(
        fileSource: FileSource,
        directoryName: String,
    ): String {
        return withContext(dispatcherProvider.IO) {
            val destinationPath = "${getDirectoryPath(directoryName)}/${NSUUID.UUID().UUIDString}"
            val typeIdentifiers = fileSource.itemProvider.registeredTypeIdentifiers
                .filterIsInstance<String>()
            val decodableTypeIdentifier = typeIdentifiers
                .firstOrNull { it in DECODABLE_TYPE_IDENTIFIERS }
            val typeIdentifier = decodableTypeIdentifier
                ?: typeIdentifiers.firstOrNull()
                ?: error("Selected file has no supported data representation")

            fileSource.itemProvider.copyTo(destinationPath, typeIdentifier)

            if (decodableTypeIdentifier == null) {
                try {
                    transcodeToJpeg(path = destinationPath, typeIdentifier = typeIdentifier)
                } catch (error: IllegalStateException) {
                    /** The copy is unusable without transcoding, so it must not stay in cache. */
                    NSFileManager.defaultManager.removeItemAtPath(
                        path = destinationPath,
                        error = null,
                    )
                    throw error
                }
            }

            destinationPath
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun getDirectoryPath(name: String): String {
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

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun clearDirectory(name: String) {
        withContext(dispatcherProvider.IO) {
            val directoryPath = "$cacheDirectoryPath/$name"
            NSFileManager.defaultManager.removeItemAtPath(
                path = directoryPath,
                error = null,
            )
            createDirectory(directoryPath)
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
    private fun transcodeToJpeg(path: String, typeIdentifier: String) {
        val image = UIImage.imageWithContentsOfFile(path = path)
            ?: error("Cannot decode selected file of type $typeIdentifier")

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
        } ?: error("Cannot redraw selected file of type $typeIdentifier")

        val jpegData = UIImageJPEGRepresentation(uprightImage, JPEG_COMPRESSION_QUALITY)
            ?: error("Cannot encode selected file of type $typeIdentifier to JPEG")

        if (!jpegData.writeToFile(path = path, atomically = true)) {
            error("Cannot write transcoded image to $path")
        }
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

/** Uniform type identifiers Skia is able to decode, preferred over whatever the picker offers. */
private val DECODABLE_TYPE_IDENTIFIERS = setOf("public.jpeg", "public.png")

private const val JPEG_COMPRESSION_QUALITY = 0.9
