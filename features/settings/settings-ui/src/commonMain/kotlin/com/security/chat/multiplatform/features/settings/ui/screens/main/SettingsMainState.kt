package com.security.chat.multiplatform.features.settings.ui.screens.main

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.features.settings.ui.screens.main.entity.DialogData
import com.security.chat.multiplatform.features.settings.ui.screens.main.entity.SettingItem

@Immutable
internal data class SettingsMainState(
    val items: List<SettingItem>,
    val dialogData: DialogData?,
    val requestInProgress: Boolean,
)