package com.security.chat.multiplatform.features.chat.data.network.di

import com.security.chat.multiplatform.features.chat.data.network.ChatNetworkManager
import com.security.chat.multiplatform.features.chat.data.network.ChatNetworkManagerImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val chatDataNetworkModule: Module =
    module {
        singleOf(::ChatNetworkManagerImpl) bind ChatNetworkManager::class
    }