package com.security.chat.multiplatform.features.settings.ui.screens.theme.mapper

import com.security.chat.multiplatform.features.settings.domain.entity.Theme
import com.security.chat.multiplatform.features.settings.ui.screens.theme.entity.ThemeItem

internal fun ThemeItem.Type.toDomain(): Theme {
    return when (this) {
        ThemeItem.Type.Auto -> Theme.Auto
        ThemeItem.Type.Dark -> Theme.Dark
        ThemeItem.Type.Light -> Theme.Light
    }
}

internal fun Theme.toUi(): ThemeItem.Type {
    return when (this) {
        Theme.Auto -> ThemeItem.Type.Auto
        Theme.Dark -> ThemeItem.Type.Dark
        Theme.Light -> ThemeItem.Type.Light
    }
}