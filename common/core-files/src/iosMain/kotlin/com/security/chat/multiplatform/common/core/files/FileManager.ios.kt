package com.security.chat.multiplatform.common.core.files

import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.withContext
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSItemProvider
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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
            val typeIdentifier =
                fileSource.itemProvider.registeredTypeIdentifiers.firstOrNull() as? String
                    ?: error("Selected file has no supported data representation")

            fileSource.itemProvider.copyTo(destinationPath, typeIdentifier)

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

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun moveFile(sourcePath: String, destinationPath: String) {
        withContext(dispatcherProvider.IO) {
            NSFileManager.defaultManager.removeItemAtPath(
                path = destinationPath,
                error = null,
            )
            NSFileManager.defaultManager.moveItemAtPath(
                srcPath = sourcePath,
                toPath = destinationPath,
                error = null,
            )
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
    private suspend fun NSItemProvider.copyTo(
        destinationPath: String,
        typeIdentifier: String,
    ) {
        suspendCoroutine { continuation ->
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
