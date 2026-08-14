package com.security.chat.multiplatform.features.profile.data

import com.security.chat.multiplatform.common.core.network.LogoutErrorAlerter
import com.security.chat.multiplatform.common.encryption.RsaSqueezer
import com.security.chat.multiplatform.features.profile.domain.entity.Profile
import com.security.chat.multiplatform.features.profile.domain.entity.UpdateProfileParams
import com.security.chat.multiplatform.features.profile.domain.repo.ProfileRepo
import com.security.chat.multiplatform.features.user.data.network.UserNetworkManager
import com.security.chat.multiplatform.features.user.data.network.entity.UpdateProfileNM
import com.security.chat.multiplatform.features.user.data.storage.UserStorage

internal class ProfileRepoImpl(
    private val userStorage: UserStorage,
    private val userNetworkManager: UserNetworkManager,
    private val logoutErrorAlerter: LogoutErrorAlerter,
) : ProfileRepo {

    override suspend fun fetchUserInfo() {
        val userId = requireNotNull(userStorage.getUserId())
        val profile = userNetworkManager.getProfile(userId)
        userStorage.saveUserName(profile.login)
    }

    override suspend fun getProfile(): Profile? {
        val name = userStorage.getUserName() ?: return null
        val privateKey = userStorage.getKeys()?.privateKey ?: return null

        return Profile(
            name = name,
            privateKey = RsaSqueezer.squeeze(privateKey),
        )
    }

    override suspend fun updateProfile(params: UpdateProfileParams) {
        val profile = userNetworkManager.updateProfile(
            params = UpdateProfileNM(
                name = params.name,
            ),
        )
        userStorage.saveUserName(profile.login)
    }

    override suspend fun deleteProfile() {
        userNetworkManager.deleteProfile()
        logoutErrorAlerter.logout()
    }
}
