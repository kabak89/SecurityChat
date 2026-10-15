package com.security.chat.multiplatform.features.call.data.di

import com.security.chat.multiplatform.features.call.data.CallManagerImpl
import com.security.chat.multiplatform.features.call.data.CallRepoImpl
import com.security.chat.multiplatform.features.call.domain.CallManager
import com.security.chat.multiplatform.features.call.domain.repo.CallRepo
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val callDataModule: Module =
    module {
        singleOf(::CallManagerImpl) bind CallManager::class
        singleOf(::CallRepoImpl) bind CallRepo::class
    }
