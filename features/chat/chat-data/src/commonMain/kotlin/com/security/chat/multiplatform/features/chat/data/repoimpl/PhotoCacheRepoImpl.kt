package com.security.chat.multiplatform.features.chat.data.repoimpl

import com.security.chat.multiplatform.common.core.files.FileManager
import com.security.chat.multiplatform.features.chat.domain.entity.CachedPhoto
import com.security.chat.multiplatform.features.chat.domain.entity.PickedPhoto
import com.security.chat.multiplatform.features.chat.domain.entity.toFileSource
import com.security.chat.multiplatform.features.chat.domain.repo.PhotoCacheRepo

internal class PhotoCacheRepoImpl(
    private val fileManager: FileManager,
) : PhotoCacheRepo {

    override suspend fun copyPhotoToCache(photo: PickedPhoto): CachedPhoto {
        val localPath = fileManager.copyToCache(
            fileSource = photo.toFileSource(),
            directoryName = "photos",
        )
        return CachedPhoto(localPath = localPath)
    }
}
