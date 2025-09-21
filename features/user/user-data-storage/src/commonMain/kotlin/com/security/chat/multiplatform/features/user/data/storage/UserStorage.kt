package com.security.chat.multiplatform.features.user.data.storage

import com.security.chat.multiplatform.common.settings.EncryptedSettings
import com.security.chat.multiplatform.features.user.data.storage.entity.CryptoKeys

public interface UserStorage {

    public suspend fun isUserAuthorized(): Boolean
    public suspend fun saveUserId(userId: String)
    public suspend fun getUserId(): String?
    public suspend fun clearUserId()
    public suspend fun saveKeys(cryptoKeys: CryptoKeys)
    public suspend fun getKeys(): CryptoKeys?

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

    override suspend fun getUserId(): String? {
        return encryptedSettings.getString(key = KEY_USER_ID)
    }

    override suspend fun clearUserId() {
        encryptedSettings.putString(
            key = KEY_USER_ID,
            value = null,
        )
    }

    override suspend fun saveKeys(cryptoKeys: CryptoKeys) {
        encryptedSettings.putString(
            key = KEY_PUBLIC_KEY,
            value = cryptoKeys.publicKey,
        )
        encryptedSettings.putString(
            key = KEY_PRIVATE_KEY,
            value = cryptoKeys.privateKey,
        )
    }

    override suspend fun getKeys(): CryptoKeys? {
        val publicKey = encryptedSettings.getString(
            key = KEY_PUBLIC_KEY,
        ) ?: return null

        val privateKey = encryptedSettings.getString(
            key = KEY_PRIVATE_KEY,
        ) ?: return null

        return CryptoKeys(
            publicKey = publicKey,
            privateKey = privateKey,
        )
    }

}

private const val KEY_USER_ID = "KEY_USER_ID"
private const val KEY_PUBLIC_KEY = "KEY_PUBLIC_KEY"
private const val KEY_PRIVATE_KEY = "KEY_PRIVATE_KEY"