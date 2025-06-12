package com.security.chat.multiplatform.features.authorize.ui.di

import com.security.chat.multiplatform.features.authorize.component.SCOPE_ID_SIGN_IN
import com.security.chat.multiplatform.features.authorize.component.SCOPE_ID_SIGN_UP
import com.security.chat.multiplatform.features.authorize.ui.screens.signin.SignInViewModel
import com.security.chat.multiplatform.features.authorize.ui.screens.signup.SignUpViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authorizeUiModule: Module =
    module {
        viewModel {
            SignUpViewModel(
                signUpModel = getScope(SCOPE_ID_SIGN_UP).get(),
            )
        }

        viewModel {
            SignInViewModel(
                signInModel = getScope(SCOPE_ID_SIGN_IN).get(),
            )
        }
    }