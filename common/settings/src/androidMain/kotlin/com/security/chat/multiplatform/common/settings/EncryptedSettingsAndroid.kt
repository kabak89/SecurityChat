package com.security.chat.multiplatform.common.settings

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.get
import com.russhwolf.settings.set

internal class EncryptedSettingsAndroid(
    private val context: Context,
) : EncryptedSettings {

    private val encryptedSettings: Settings by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedPreferences = EncryptedSharedPreferences.create(
            /* context = */
            context,
            /* fileName = */
            context.packageName + "_encrypted_preferences",
            /* masterKey = */
            masterKey,
            /* prefKeyEncryptionScheme = */
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            /* prefValueEncryptionScheme = */
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

        SharedPreferencesSettings(encryptedPreferences)
    }

    override fun putString(key: String, value: String?) {
        encryptedSettings[key] = value
    }

    override fun getString(key: String): String? {
        return encryptedSettings[key]
    }

    override fun putLong(key: String, value: Long?) {
        encryptedSettings[key] = value
    }

    override fun getLong(key: String): Long? {
        return encryptedSettings[key]
    }

    override fun putDouble(key: String, value: Double?) {
        encryptedSettings[key] = value
    }

    override fun getDouble(key: String): Double? {
        return encryptedSettings[key]
    }

    override fun putBoolean(key: String, value: Boolean?) {
        encryptedSettings[key] = value
    }

    override fun getBoolean(key: String): Boolean? {
        return encryptedSettings[key]
    }
}