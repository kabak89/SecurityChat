package com.security.chat.multiplatform.features.chat.data.di

import com.security.chat.multiplatform.features.chat.data.repoimpl.ChatRepoImpl
import com.security.chat.multiplatform.features.chat.domain.repo.ChatRepo
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val chatDataModule: Module =
    module {
        singleOf(::ChatRepoImpl) bind ChatRepo::class
    }