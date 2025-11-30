package com.security.chat.multiplatform.features.chat.data.storage.di

import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.chat.data.storage.ChatStorageImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val chatDataStorageModule: Module =
    module {
        singleOf(::ChatStorageImpl) bind ChatStorage::class
    }