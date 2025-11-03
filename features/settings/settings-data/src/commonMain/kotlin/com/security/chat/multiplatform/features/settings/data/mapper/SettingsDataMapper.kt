package com.security.chat.multiplatform.features.settings.data.mapper

import com.security.chat.multiplatform.features.settings.data.storage.entity.ThemeSM
import com.security.chat.multiplatform.features.settings.domain.entity.Theme

internal fun Theme.toSm(): ThemeSM {
    return when (this) {
        Theme.Auto -> ThemeSM.Auto
        Theme.Dark -> ThemeSM.Dark
        Theme.Light -> ThemeSM.Light
    }
}

internal fun ThemeSM.toDomain(): Theme {
    return when (this) {
        ThemeSM.Auto -> Theme.Auto
        ThemeSM.Dark -> Theme.Dark
        ThemeSM.Light -> Theme.Light
    }
}