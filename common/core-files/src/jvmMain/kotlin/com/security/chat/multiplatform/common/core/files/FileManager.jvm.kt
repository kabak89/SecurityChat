package com.security.chat.multiplatform.common.core.files

import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

internal class FileManagerJvm(
    private val dispatcherProvider: DispatcherProviderInterface,
) : FileManager {

    private val cacheDirectory = File(System.getProperty("user.home"), ".SecurityChat/cache")
    private val dataDirectory = File(System.getProperty("user.home"), ".SecurityChat/data")

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
}
