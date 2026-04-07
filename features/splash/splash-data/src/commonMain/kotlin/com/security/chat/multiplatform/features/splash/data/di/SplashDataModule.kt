package com.security.chat.multiplatform.features.splash.data.di

import com.security.chat.multiplatform.features.splash.data.repoimpl.SplashRepoImpl
import com.security.chat.multiplatform.features.splash.domain.repo.SplashRepo
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val splashDataModule: Module =
    module {
        singleOf(::SplashRepoImpl) bind SplashRepo::class
    }