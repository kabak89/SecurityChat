package com.security.chat.multiplatform.features.chats.data.network.di

import com.security.chat.multiplatform.features.chats.data.network.ChatsNetworkManager
import com.security.chat.multiplatform.features.chats.data.network.ChatsNetworkManagerImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val chatsDataNetworkModule: Module =
    module {
        singleOf(::ChatsNetworkManagerImpl) bind ChatsNetworkManager::class
    }
