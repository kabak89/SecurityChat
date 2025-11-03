package com.security.chat.multiplatform.features.settings.ui.screens.theme.entity

import androidx.compose.runtime.Immutable

@Immutable
internal data class ThemeItem(
    val type: Type,
    val title: String,
    val enabled: Boolean,
) {
    internal enum class Type {
        Auto,
        Dark,
        Light,
    }
}