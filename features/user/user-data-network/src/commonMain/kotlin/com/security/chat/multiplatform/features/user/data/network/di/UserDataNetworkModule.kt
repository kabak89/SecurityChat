package com.security.chat.multiplatform.features.user.data.network.di

import com.security.chat.multiplatform.features.user.data.network.UserNetworkManager
import com.security.chat.multiplatform.features.user.data.network.UserNetworkManagerImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val userNetworkManager: Module =
    module {
        singleOf(::UserNetworkManagerImpl) bind UserNetworkManager::class
    }