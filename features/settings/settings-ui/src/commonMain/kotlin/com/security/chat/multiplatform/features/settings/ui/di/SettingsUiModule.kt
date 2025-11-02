package com.security.chat.multiplatform.features.settings.ui.di

import com.security.chat.multiplatform.features.settings.ui.screens.main.SettingsMainViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

public val settingsUiModule: Module =
    module {
        viewModel {
            SettingsMainViewModel(
//                chatModel = getScope(SCOPE_ID_CHAT).get(),
//                params = get(),
            )
        }
    }