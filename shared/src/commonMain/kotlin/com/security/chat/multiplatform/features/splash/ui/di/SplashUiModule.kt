package com.security.chat.multiplatform.features.splash.ui.di

import com.security.chat.multiplatform.features.splash.ui.screens.splash.SplashViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val splashUiModule: Module =
    module {
        viewModelOf(::SplashViewModel)
    }