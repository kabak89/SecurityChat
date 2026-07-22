package com.security.chat.multiplatform.common.core.files

public interface FileManager {

    public suspend fun copyToCache(
        fileSource: FileSource,
        directoryName: String,
    ): String

    public suspend fun getDirectoryPath(name: String): String

    /**
     * Returns the path of a directory in the app's persistent data storage (survives cache
     * eviction), creating it if needed.
     */
    public suspend fun getDataDirectoryPath(name: String): String

    /** Moves the file at [sourcePath] to [destinationPath], overwriting any existing destination. */
    public suspend fun moveFile(sourcePath: String, destinationPath: String)

    public suspend fun clearDirectory(name: String)
    public suspend fun deleteFile(path: String)
}
