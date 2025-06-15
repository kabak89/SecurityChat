package com.security.chat.multiplatform.common.settings

/**
 * Common interface for saving primitive data
 */
public interface SettingsManager {

    public fun putString(key: String, value: String?)
    public fun getString(key: String): String?

    public fun putLong(key: String, value: Long?)
    public fun getLong(key: String): Long?

    public fun putDouble(key: String, value: Double?)
    public fun getDouble(key: String): Double?

    public fun putBoolean(key: String, value: Boolean?)
    public fun getBoolean(key: String): Boolean?

}

/**
 * Interface to save encrypted primitive data
 */
public interface EncryptedSettings : SettingsManager

/**
 * Interface to save non sensitive not encrypted primitive data
 */
public interface PublicSettings : SettingsManager