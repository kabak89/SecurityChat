package com.security.chat.multiplatform.features.authorize.ui.di

import com.security.chat.multiplatform.features.authorize.ui.screens.login.LoginViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authorizeUiModule: Module =
    module {
        viewModelOf(::LoginViewModel)
    }