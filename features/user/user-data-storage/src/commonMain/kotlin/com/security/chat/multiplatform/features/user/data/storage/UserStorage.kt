package com.security.chat.multiplatform.features.user.data.storage

import com.security.chat.multiplatform.common.settings.EncryptedSettings

public interface UserStorage {

    public suspend fun isUserAuthorized(): Boolean
    public suspend fun saveUserId(userId: String)
    public suspend fun clearUserId()

}

internal class UserStorageImpl(
    private val encryptedSettings: EncryptedSettings,
) : UserStorage {

    override suspend fun isUserAuthorized(): Boolean {
        return encryptedSettings.getString(KEY_USER_ID) != null
    }

    override suspend fun saveUserId(userId: String) {
        encryptedSettings.putString(
            key = KEY_USER_ID,
            value = userId,
        )
    }

    override suspend fun clearUserId() {
        encryptedSettings.putString(
            key = KEY_USER_ID,
            value = null,
        )
    }
}

private const val KEY_USER_ID = "KEY_USER_ID"