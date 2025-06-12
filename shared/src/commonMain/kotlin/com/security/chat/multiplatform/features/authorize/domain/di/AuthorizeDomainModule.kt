package com.security.chat.multiplatform.features.authorize.domain.di

import com.security.chat.multiplatform.features.authorize.component.SCOPE_ID_SIGN_IN
import com.security.chat.multiplatform.features.authorize.component.SCOPE_ID_SIGN_UP
import com.security.chat.multiplatform.features.authorize.domain.SignInModel
import com.security.chat.multiplatform.features.authorize.domain.SignInModelImpl
import com.security.chat.multiplatform.features.authorize.domain.SignUpModel
import com.security.chat.multiplatform.features.authorize.domain.SignUpModelImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val authorizeDomainModule: Module =
    module {
        scope(named(SCOPE_ID_SIGN_IN)) {
            scopedOf(::SignInModelImpl) bind SignInModel::class
        }

        scope(named(SCOPE_ID_SIGN_UP)) {
            scopedOf(::SignUpModelImpl) bind SignUpModel::class
        }
    }