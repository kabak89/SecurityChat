package com.security.chat.multiplatform.features.authorize.domain.di

import com.security.chat.multiplatform.features.authorize.component.SCOPE_ID_SIGN_UP
import com.security.chat.multiplatform.features.authorize.domain.SignInModel
import com.security.chat.multiplatform.features.authorize.domain.SignInModelImpl
import com.security.chat.multiplatform.features.authorize.domain.SignUpModel
import com.security.chat.multiplatform.features.authorize.domain.SignUpModelImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val authorizeDomainModule: Module =
    module {
        singleOf(::SignInModelImpl) bind SignInModel::class

        scope(named(SCOPE_ID_SIGN_UP)) {
            scoped {
                SignUpModelImpl(
                    signUpRepo = get(),
                    dispatcherProvider = get(),
                )
                    .apply {
                        start(parentScope = get())
                    }
            } bind SignUpModel::class
        }
    }