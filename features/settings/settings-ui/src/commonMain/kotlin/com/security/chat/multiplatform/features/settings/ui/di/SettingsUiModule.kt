package com.security.chat.multiplatform.features.settings.ui.di

import com.security.chat.multiplatform.features.settings.ui.screens.main.SettingsMainViewModel
import com.security.chat.multiplatform.features.settings.ui.screens.theme.ThemeViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

public val settingsUiModule: Module =
    module {
        viewModelOf(::SettingsMainViewModel)
        viewModelOf(::ThemeViewModel)
    }