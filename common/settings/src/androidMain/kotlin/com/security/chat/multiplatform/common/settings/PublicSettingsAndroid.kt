package com.security.chat.multiplatform.common.settings

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.get
import com.russhwolf.settings.set

internal class PublicSettingsAndroid(
    private val context: Context,
) : PublicSettings {

    private val publicSettings: Settings by lazy {
        SharedPreferencesSettings.Factory(context = context).create()
    }

    override fun putString(key: String, value: String?) {
        publicSettings[key] = value
    }

    override fun getString(key: String): String? {
        return publicSettings[key]
    }

    override fun putLong(key: String, value: Long?) {
        publicSettings[key] = value
    }

    override fun getLong(key: String): Long? {
        return publicSettings[key]
    }

    override fun putDouble(key: String, value: Double?) {
        publicSettings[key] = value
    }

    override fun getDouble(key: String): Double? {
        return publicSettings[key]
    }

    override fun putBoolean(key: String, value: Boolean?) {
        publicSettings[key] = value
    }

    override fun getBoolean(key: String): Boolean? {
        return publicSettings[key]
    }

}