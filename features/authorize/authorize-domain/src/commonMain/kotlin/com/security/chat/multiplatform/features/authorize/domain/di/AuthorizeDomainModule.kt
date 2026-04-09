package com.security.chat.multiplatform.features.authorize.domain.di

import com.security.chat.multiplatform.features.authorize.domain.SignInModel
import com.security.chat.multiplatform.features.authorize.domain.SignInModelImpl
import com.security.chat.multiplatform.features.authorize.domain.SignUpModel
import com.security.chat.multiplatform.features.authorize.domain.SignUpModelImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val authorizeDomainModule: Module =
    module {
        singleOf(::SignInModelImpl) bind SignInModel::class
        singleOf(::SignUpModelImpl) bind SignUpModel::class
    }