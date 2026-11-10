package com.security.chat.multiplatform.features.settings.ui.screens.main.entity

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.entity.PrintableText
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import securitychat.common.localization.generated.resources.settings_main_log_out
import securitychat.common.localization.generated.resources.settings_main_logs
import securitychat.common.localization.generated.resources.settings_main_permissions
import securitychat.common.localization.generated.resources.settings_main_profile
import securitychat.common.localization.generated.resources.settings_main_theme

@Immutable
internal sealed interface SettingItem {

    val title: PrintableText

    data object Profile : SettingItem {
        override val title: PrintableText
            get() = resPrintableText(StringRes.settings_main_profile)
    }

    data object Theme : SettingItem {
        override val title: PrintableText
            get() = resPrintableText(StringRes.settings_main_theme)
    }

    data object Logout : SettingItem {
        override val title: PrintableText
            get() = resPrintableText(StringRes.settings_main_log_out)
    }

    data object Notifications : SettingItem {
        override val title: PrintableText
            get() = resPrintableText(StringRes.settings_main_permissions)
    }

    data object Logs : SettingItem {
        override val title: PrintableText
            get() = resPrintableText(StringRes.settings_main_logs)
    }
}

internal fun settingsMainItems(showLogs: Boolean): List<SettingItem> {
    return buildList {
        add(SettingItem.Profile)
        add(SettingItem.Theme)
        add(SettingItem.Notifications)
        if (showLogs) add(SettingItem.Logs)
        add(SettingItem.Logout)
    }
}
