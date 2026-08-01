package com.security.chat.multiplatform.common.core.files

public interface FileManager {

    public suspend fun isImage(fileSource: FileSource): Boolean
    public suspend fun isRenderable(path: String): Boolean
    public suspend fun transcodeToJpeg(path: String)

    public suspend fun copyToCache(
        fileSource: FileSource,
        directoryName: String,
    ): String

    public suspend fun getDirectoryPath(name: String): String
    public suspend fun getDataDirectoryPath(name: String): String
    public suspend fun moveFile(sourcePath: String, destinationPath: String)
    public suspend fun clearDirectory(name: String)
    public suspend fun deleteFile(path: String)
    public suspend fun getImagesDirectoryPath(): String = getDataDirectoryPath(IMAGES_FOLDER)

    public companion object {
        public const val IMAGES_FOLDER: String = "images"
        public const val ENCRYPTED_IMAGES_FOLDER: String = "encrypted_images"
    }
}
