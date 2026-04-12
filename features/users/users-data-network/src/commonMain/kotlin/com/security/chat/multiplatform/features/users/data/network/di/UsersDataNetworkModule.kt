package com.security.chat.multiplatform.features.users.data.network.di

import com.security.chat.multiplatform.features.users.data.network.UsersNetworkManager
import com.security.chat.multiplatform.features.users.data.network.UsersNetworkManagerImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val usersNetworkManager: Module =
    module {
        singleOf(::UsersNetworkManagerImpl) bind UsersNetworkManager::class
    }