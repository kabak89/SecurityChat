package com.security.chat.multiplatform.features.push.data.di

import com.security.chat.multiplatform.features.push.data.PushRepositoryImpl
import com.security.chat.multiplatform.features.push.domain.PushRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val pushDataModule: Module =
    module {
        singleOf(::PushRepositoryImpl) bind PushRepository::class
    }
