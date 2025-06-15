package com.security.chat.multiplatform.features.user.data.storage.di

import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import com.security.chat.multiplatform.features.user.data.storage.UserStorageImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val userDataStorageModule: Module =
    module {
        singleOf(::UserStorageImpl) bind UserStorage::class
    }