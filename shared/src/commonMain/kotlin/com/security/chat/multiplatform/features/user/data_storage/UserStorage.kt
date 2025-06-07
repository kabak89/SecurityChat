package com.security.chat.multiplatform.features.user.data_storage

import com.security.chat.multiplatform.common.settings.EncryptedSettings

interface UserStorage {

    suspend fun isUserAuthorized(): Boolean

}

class UserStorageImpl(
    private val encryptedSettings: EncryptedSettings,
) : UserStorage {

    override suspend fun isUserAuthorized(): Boolean {
        return encryptedSettings.getString(KEY_USER_ID) != null
    }

}

private const val KEY_USER_ID = "KEY_USER_ID"