package com.security.chat.multiplatform.features.push.domain.di

import com.security.chat.multiplatform.features.push.domain.PushModel
import com.security.chat.multiplatform.features.push.domain.PushModelImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val pushDomainModule: Module =
    module {
        singleOf(::PushModelImpl) bind PushModel::class
    }