package com.security.chat.multiplatform.features.user.data_storage.di

import com.security.chat.multiplatform.features.user.data_storage.UserStorage
import com.security.chat.multiplatform.features.user.data_storage.UserStorageImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val userDataStorageModule: Module =
    module {
        singleOf(::UserStorageImpl) bind UserStorage::class
    }