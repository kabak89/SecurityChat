package com.security.chat.multiplatform.features.profile.domain.repo

import com.security.chat.multiplatform.features.profile.domain.entity.Profile
import com.security.chat.multiplatform.features.profile.domain.entity.UpdateProfileParams

public interface ProfileRepo {
    public suspend fun fetchUserInfo()
    public suspend fun getProfile(): Profile?
    public suspend fun updateProfile(params: UpdateProfileParams)
}
