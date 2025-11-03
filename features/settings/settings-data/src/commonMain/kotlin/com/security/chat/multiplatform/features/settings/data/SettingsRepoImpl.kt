package com.security.chat.multiplatform.features.settings.data

import com.security.chat.multiplatform.features.settings.domain.repo.SettingsRepo
import com.security.chat.multiplatform.features.user.data.storage.UserStorage

internal class SettingsRepoImpl(
    private val userStorage: UserStorage,
) : SettingsRepo {

    override suspend fun logout() {
        userStorage.clearUserId()
        userStorage.clearKeys()
    }

}