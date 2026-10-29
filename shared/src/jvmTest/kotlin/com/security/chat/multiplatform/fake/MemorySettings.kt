package com.security.chat.multiplatform.fake

import com.security.chat.multiplatform.common.settings.EncryptedSettings
import com.security.chat.multiplatform.common.settings.PublicSettings

internal class MemorySettings : EncryptedSettings, PublicSettings {
    private val values = mutableMapOf<String, Any?>()

    override fun putString(key: String, value: String?) {
        values[key] = value
    }

    override fun getString(key: String): String? = values[key] as? String

    override fun putLong(key: String, value: Long?) {
        values[key] = value
    }

    override fun getLong(key: String): Long? = values[key] as? Long

    override fun putDouble(key: String, value: Double?) {
        values[key] = value
    }

    override fun getDouble(key: String): Double? = values[key] as? Double

    override fun putBoolean(key: String, value: Boolean?) {
        values[key] = value
    }

    override fun getBoolean(key: String): Boolean? = values[key] as? Boolean
}
