package com.security.chat.multiplatform.features.settings.ui.screens.theme

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.features.settings.ui.screens.theme.entity.ThemeItem

@Immutable
internal data class ThemeState(
    val items: List<ThemeItem>,
)