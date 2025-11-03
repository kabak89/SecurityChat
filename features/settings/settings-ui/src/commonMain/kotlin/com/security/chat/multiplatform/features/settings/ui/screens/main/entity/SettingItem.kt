package com.security.chat.multiplatform.features.settings.ui.screens.main.entity

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface SettingItem {
    val title: String

    data class Theme(
        override val title: String,
    ) : SettingItem

    data class Logout(
        override val title: String,
    ) : SettingItem

}