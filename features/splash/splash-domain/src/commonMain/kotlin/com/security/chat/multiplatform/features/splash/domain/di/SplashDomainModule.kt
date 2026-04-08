package com.security.chat.multiplatform.features.splash.domain.di

import com.security.chat.multiplatform.features.splash.domain.SplashModel
import com.security.chat.multiplatform.features.splash.domain.SplashModelImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val splashDomainModule: Module =
    module {
        singleOf(::SplashModelImpl) bind SplashModel::class
    }