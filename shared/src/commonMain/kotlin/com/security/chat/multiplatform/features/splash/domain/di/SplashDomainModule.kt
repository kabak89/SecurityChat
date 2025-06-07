package com.security.chat.multiplatform.features.splash.domain.di

import com.security.chat.multiplatform.features.splash.component.SCOPE_ID_SPLASH
import com.security.chat.multiplatform.features.splash.domain.SplashModel
import com.security.chat.multiplatform.features.splash.domain.SplashModelImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val splashDomainModule: Module =
    module {
        scope(named(SCOPE_ID_SPLASH)) {
            scopedOf(::SplashModelImpl) bind SplashModel::class
        }
    }