package com.security.chat.multiplatform.features.settings.ui.screens.main.entity

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.PrintableText
import com.security.chat.multiplatform.common.core.ui.resPrintableText
import securitychat.common.localization.generated.resources.settings_main_log_out
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
}