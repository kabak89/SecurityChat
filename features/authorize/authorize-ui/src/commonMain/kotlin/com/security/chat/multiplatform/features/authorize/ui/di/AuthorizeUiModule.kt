package com.security.chat.multiplatform.features.authorize.ui.di

import com.security.chat.multiplatform.features.authorize.ui.screens.signin.SignInViewModel
import com.security.chat.multiplatform.features.authorize.ui.screens.signup.SignUpViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

public val authorizeUiModule: Module =
    module {
        viewModelOf(::SignUpViewModel)
        viewModelOf(::SignInViewModel)
    }