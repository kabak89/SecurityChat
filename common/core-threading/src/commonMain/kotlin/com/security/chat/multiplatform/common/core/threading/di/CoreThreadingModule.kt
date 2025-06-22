package com.security.chat.multiplatform.common.core.threading.di

import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterfaceImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val coreThreadingModule: Module =
    module {
        singleOf(::DispatcherProviderInterfaceImpl) bind DispatcherProviderInterface::class
    }