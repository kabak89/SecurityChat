package com.security.chat.multiplatform.features.settings.ui.screens.main.entity

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface SettingItem {
    val title: String

    data object Profile : SettingItem {
        override val title: String
            get() = "Profile"
    }

    data object Theme : SettingItem {
        override val title: String
            get() = "Theme"
    }

    data object Logout : SettingItem {
        override val title: String
            get() = "Log out"
    }

}