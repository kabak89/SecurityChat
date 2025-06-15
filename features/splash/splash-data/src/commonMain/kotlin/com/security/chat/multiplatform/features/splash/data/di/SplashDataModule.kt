package com.security.chat.multiplatform.features.splash.data.di

import com.security.chat.multiplatform.features.splash.component.SCOPE_ID_SPLASH
import com.security.chat.multiplatform.features.splash.data.repoimpl.SplashRepoImpl
import com.security.chat.multiplatform.features.splash.domain.repo.SplashRepo
import org.koin.core.module.Module
import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

public val splashDataModule: Module =
    module {
        scope(named(SCOPE_ID_SPLASH)) {
            scopedOf(::SplashRepoImpl) bind SplashRepo::class
        }
    }