package com.security.chat.multiplatform.features.chats.data.storage.di

import com.security.chat.multiplatform.features.chats.data.storage.ChatsStorage
import com.security.chat.multiplatform.features.chats.data.storage.ChatsStorageImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val chatsDataStorageModule: Module =
    module {
        singleOf(::ChatsStorageImpl) bind ChatsStorage::class
    }