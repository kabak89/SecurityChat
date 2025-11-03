package com.security.chat.multiplatform.features.settings.ui.di

import com.security.chat.multiplatform.features.settings.component.SCOPE_ID_SETTINGS
import com.security.chat.multiplatform.features.settings.ui.screens.main.SettingsMainViewModel
import com.security.chat.multiplatform.features.settings.ui.screens.theme.ThemeViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

public val settingsUiModule: Module =
    module {
        viewModel {
            SettingsMainViewModel(
                settingsModel = getScope(SCOPE_ID_SETTINGS).get(),
            )
        }
        viewModel {
            ThemeViewModel(
                settingsModel = getScope(SCOPE_ID_SETTINGS).get(),
            )
        }
    }