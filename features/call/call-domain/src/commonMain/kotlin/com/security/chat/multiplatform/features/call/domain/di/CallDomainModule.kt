package com.security.chat.multiplatform.features.call.domain.di

import com.security.chat.multiplatform.features.call.domain.CallModel
import com.security.chat.multiplatform.features.call.domain.CallModelImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val callDomainModule: Module =
    module {
        singleOf(::CallModelImpl) bind CallModel::class
    }
