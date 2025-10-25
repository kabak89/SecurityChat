package com.security.chat.multiplatform.features.users.data.storage.di

import com.security.chat.multiplatform.features.users.data.storage.UsersStorage
import com.security.chat.multiplatform.features.users.data.storage.UsersStorageImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val usersDataStorageModule: Module =
    module {
        singleOf(::UsersStorageImpl) bind UsersStorage::class
    }