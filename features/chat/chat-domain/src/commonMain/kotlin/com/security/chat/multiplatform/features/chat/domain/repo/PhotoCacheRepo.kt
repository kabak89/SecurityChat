package com.security.chat.multiplatform.features.chat.domain.repo

import com.security.chat.multiplatform.features.chat.domain.entity.CachedPhoto
import com.security.chat.multiplatform.features.chat.domain.entity.PickedPhoto

public interface PhotoCacheRepo {

    public suspend fun copyPhotoToCache(photo: PickedPhoto): CachedPhoto
}
