package com.security.chat.multiplatform.common.core.files

import com.security.chat.multiplatform.common.core.files.FileManager.Companion.DOWNLOADS_FOLDER
import com.security.chat.multiplatform.common.core.files.FileManager.Companion.ENCRYPTED_IMAGES_FOLDER
import com.security.chat.multiplatform.common.core.files.FileManager.Companion.IMAGES_FOLDER

public interface FileManager {

    public suspend fun isImage(fileSource: FileSource): Boolean
    public suspend fun isRenderable(path: String): Boolean
    public suspend fun transcodeToJpeg(path: String)

    public suspend fun copyToCache(
        fileSource: FileSource,
        directoryName: String,
    ): String

    public suspend fun getCacheDirectoryPath(name: String): String
    public suspend fun getDataDirectoryPath(name: String): String
    public suspend fun moveFile(sourcePath: String, destinationPath: String)
    public suspend fun clearCacheDirectory(name: String)
    public suspend fun clearDataDirectory(name: String)
    public suspend fun deleteFile(path: String)
    public suspend fun fileExists(path: String): Boolean
    public suspend fun getImagesDirectoryPath(): String = getDataDirectoryPath(IMAGES_FOLDER)

    public suspend fun clearAllFiles() {
        CACHE_FOLDERS.forEach { name -> clearCacheDirectory(name) }
        DATA_FOLDERS.forEach { name -> clearDataDirectory(name) }
    }

    public companion object {
        public const val IMAGES_FOLDER: String = "images"
        public const val ENCRYPTED_IMAGES_FOLDER: String = "encrypted_images"

        /** Cache-only staging area for files that are still being downloaded. */
        public const val DOWNLOADS_FOLDER: String = "downloads"
    }
}

/** [IMAGES_FOLDER] exists under both roots: picked images are staged in the cache one. */
private val CACHE_FOLDERS: List<String> = listOf(IMAGES_FOLDER, DOWNLOADS_FOLDER)
private val DATA_FOLDERS: List<String> = listOf(IMAGES_FOLDER, ENCRYPTED_IMAGES_FOLDER)
