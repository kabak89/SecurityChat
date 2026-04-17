package com.security.chat.multiplatform.features.profile.data

import com.security.chat.multiplatform.features.profile.domain.entity.Profile
import com.security.chat.multiplatform.features.profile.domain.entity.UpdateProfileParams
import com.security.chat.multiplatform.features.profile.domain.repo.ProfileRepo
import com.security.chat.multiplatform.features.user.data.network.UserNetworkManager
import com.security.chat.multiplatform.features.user.data.network.entity.UpdateProfileNM
import com.security.chat.multiplatform.features.user.data.storage.UserStorage

internal class ProfileRepoImpl(
    private val userStorage: UserStorage,
    private val userNetworkManager: UserNetworkManager,
) : ProfileRepo {

    override suspend fun fetchUserInfo() {
        val userId = requireNotNull(userStorage.getUserId())
        val profile = userNetworkManager.getProfile(userId)
        userStorage.saveUserName(profile.login)
    }

    override suspend fun getProfile(): Profile? {
        val name = userStorage.getUserName() ?: return null
        return Profile(
            name = name,
        )
    }

    override suspend fun updateProfile(params: UpdateProfileParams) {
        val userId = requireNotNull(userStorage.getUserId())
        val profile = userNetworkManager.updateProfile(
            params = UpdateProfileNM(
                id = userId,
                name = params.name,
            ),
        )
        userStorage.saveUserName(profile.login)
    }
}
