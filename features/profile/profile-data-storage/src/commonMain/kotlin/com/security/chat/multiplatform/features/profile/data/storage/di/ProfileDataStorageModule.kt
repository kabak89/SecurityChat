package com.security.chat.multiplatform.features.profile.data.storage.di

import com.security.chat.multiplatform.features.profile.data.storage.ProfileStorage
import com.security.chat.multiplatform.features.profile.data.storage.ProfileStorageImpl
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

public val profileDataStorageModule: Module =
    module {
        single { ProfileStorageImpl() } bind ProfileStorage::class
    }
