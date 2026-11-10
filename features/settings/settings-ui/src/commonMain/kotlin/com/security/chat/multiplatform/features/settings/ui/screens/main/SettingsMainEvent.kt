package com.security.chat.multiplatform.features.settings.ui.screens.main

internal sealed interface SettingsMainEvent {
    data object GoToTheme : SettingsMainEvent
    data object GoToNotifications : SettingsMainEvent
    data object GoToProfile : SettingsMainEvent
    data object GoToLogs : SettingsMainEvent
}
