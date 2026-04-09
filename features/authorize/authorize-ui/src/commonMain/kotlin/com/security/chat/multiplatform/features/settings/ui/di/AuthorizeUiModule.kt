package com.security.chat.multiplatform.features.settings.ui.di

import com.security.chat.multiplatform.features.settings.ui.screens.signin.SignInViewModel
import com.security.chat.multiplatform.features.settings.ui.screens.signup.SignUpViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

public val authorizeUiModule: Module =
    module {
        viewModelOf(::SignUpViewModel)
        viewModelOf(::SignInViewModel)
    }