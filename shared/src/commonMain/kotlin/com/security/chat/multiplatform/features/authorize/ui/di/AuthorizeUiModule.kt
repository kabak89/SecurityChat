package com.security.chat.multiplatform.features.authorize.ui.di

import com.security.chat.multiplatform.features.authorize.component.SCOPE_ID_SIGN_IN
import com.security.chat.multiplatform.features.authorize.ui.screens.login.SignUpViewModel
import com.security.chat.multiplatform.features.authorize.ui.screens.signin.SignInViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authorizeUiModule: Module =
    module {
        viewModelOf(::SignUpViewModel)

        viewModel {
            SignInViewModel(
                signInModel = getScope(SCOPE_ID_SIGN_IN).get(),
            )
        }
    }