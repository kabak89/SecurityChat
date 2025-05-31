package com.security.chat.multiplatform.features.splash.ui.di

import com.security.chat.multiplatform.features.splash.component.SCOPE_ID_SPLASH
import com.security.chat.multiplatform.features.splash.ui.screens.splash.SplashViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val splashUiModule: Module =
    module {
        viewModel {
            SplashViewModel(
                splashModel = getScope(SCOPE_ID_SPLASH).get(),
            )
        }
    }