package com.security.chat.multiplatform.common.settings

import java.security.GeneralSecurityException
import java.util.prefs.Preferences

/**
 * AES-256-GCM encrypted values in a dedicated Preferences node; master key in
 * `~/.SecurityChat/.master-key`.
 */
internal class EncryptedSettingsDesktop : EncryptedSettings {

    private val secure: Preferences =
        Preferences.userRoot().node("com/security_chat/encrypted_settings")

    private val encryptor by lazy {
        AesGcmStringEncryptor(DesktopMasterKeyProvider.getOrCreateAes256Key())
    }

    override fun putString(key: String, value: String?) {
        if (value == null) {
            secure.remove(key)
        } else {
            secure.put(key, encryptor.encrypt(value))
        }
    }

    override fun getString(key: String): String? {
        val stored = secure.get(key, null) ?: return null
        return decryptOrRemoveCorrupt(key, stored)
    }

    override fun putLong(key: String, value: Long?) {
        if (value == null) {
            secure.remove(key)
        } else {
            secure.put(key, encryptor.encrypt(value.toString()))
        }
    }

    override fun getLong(key: String): Long? {
        val stored = secure.get(key, null) ?: return null
        return decryptOrRemoveCorrupt(key, stored)?.toLongOrNull()
    }

    override fun putDouble(key: String, value: Double?) {
        if (value == null) {
            secure.remove(key)
        } else {
            secure.put(key, encryptor.encrypt(value.toString()))
        }
    }

    override fun getDouble(key: String): Double? {
        val stored = secure.get(key, null) ?: return null
        return decryptOrRemoveCorrupt(key, stored)?.toDoubleOrNull()
    }

    override fun putBoolean(key: String, value: Boolean?) {
        if (value == null) {
            secure.remove(key)
        } else {
            secure.put(key, encryptor.encrypt(value.toString()))
        }
    }

    override fun getBoolean(key: String): Boolean? {
        val stored = secure.get(key, null) ?: return null
        return decryptOrRemoveCorrupt(key, stored)?.toBooleanStrictOrNull()
    }

    /**
     * Wrong or rotated [DesktopMasterKeyProvider] key vs existing ENC1 blobs, truncated prefs,
     * or corrupt Base64 all yield crypto failures; drop the entry so callers can recreate secrets.
     */
    private fun decryptOrRemoveCorrupt(key: String, stored: String): String? {
        return try {
            encryptor.decrypt(stored)
        } catch (_: GeneralSecurityException) {
            secure.remove(key)
            null
        } catch (_: IllegalArgumentException) {
            secure.remove(key)
            null
        }
    }
}
