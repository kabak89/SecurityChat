package com.security.chat.multiplatform.common.settings

/**
 * Common interface for saving primitive data
 */
interface SettingsManager {

    fun putString(key: String, value: String?)
    fun getString(key: String): String?

    fun putLong(key: String, value: Long?)
    fun getLong(key: String): Long?

    fun putDouble(key: String, value: Double?)
    fun getDouble(key: String): Double?

    fun putBoolean(key: String, value: Boolean?)
    fun getBoolean(key: String): Boolean?

}

/**
 * Interface to save encrypted primitive data
 */
interface EncryptedSettings : SettingsManager

/**
 * Interface to save non sensitive not encrypted primitive data
 */
interface PublicSettings : SettingsManager