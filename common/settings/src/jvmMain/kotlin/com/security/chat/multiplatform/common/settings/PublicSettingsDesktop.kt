package com.security.chat.multiplatform.common.settings

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import java.util.prefs.Preferences

internal class PublicSettingsDesktop() : PublicSettings {

    private val encryptedSettings: Settings by lazy {
        PreferencesSettings(Preferences.userRoot())
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