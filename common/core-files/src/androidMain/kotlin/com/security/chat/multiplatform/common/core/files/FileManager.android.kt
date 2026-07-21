package com.security.chat.multiplatform.common.core.files

import android.content.Context
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

internal class FileManagerAndroid(
    context: Context,
    private val dispatcherProvider: DispatcherProviderInterface,
) : FileManager {

    private val cacheDirectory = context.cacheDir

    override suspend fun copyToCache(
        fileSource: FileSource,
        directoryName: String,
    ): String {
        return withContext(dispatcherProvider.IO) {
            val destinationFile = File(
                getOrCreateDirectory(directoryName),
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

    override suspend fun getDirectoryPath(name: String): String {
        return withContext(dispatcherProvider.IO) {
            getOrCreateDirectory(name).absolutePath
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
