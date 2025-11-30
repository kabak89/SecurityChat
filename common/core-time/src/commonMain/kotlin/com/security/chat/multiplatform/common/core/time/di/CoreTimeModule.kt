package com.security.chat.multiplatform.common.core.time.di

import com.security.chat.multiplatform.common.core.time.TimeProvider
import com.security.chat.multiplatform.common.core.time.TimeProviderImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val coreTimeModule: Module =
    module {
        singleOf(::TimeProviderImpl) bind TimeProvider::class
    }