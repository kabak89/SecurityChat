package com.security.chat.multiplatform.features.settings.data.storage.mapper

import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.settings.data.storage.entity.ThemeSM

internal fun mapThemeToSM(string: String?): ThemeSM {
    if (string == null) return ThemeSM.Auto

    ThemeSM.entries.forEach { entry ->
        if (mapThemeToString(entry) == string) {
            return entry
        }
    }

    Log.e("unknown theme value: $string")
    return ThemeSM.Auto
}

internal fun mapThemeToString(theme: ThemeSM): String {
    return when (theme) {
        ThemeSM.Auto -> "auto"
        ThemeSM.Dark -> "dark"
        ThemeSM.Light -> "light"
    }
}