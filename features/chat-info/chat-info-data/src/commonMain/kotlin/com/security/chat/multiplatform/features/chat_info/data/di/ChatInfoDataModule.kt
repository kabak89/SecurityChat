package com.security.chat.multiplatform.features.chat_info.data.di

import com.security.chat.multiplatform.features.chat_info.data.repository.ChatInfoRepositoryImpl
import com.security.chat.multiplatform.features.chat_info.domain.repository.ChatInfoRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val chatInfoDataModule: Module =
    module {
        singleOf(::ChatInfoRepositoryImpl) bind ChatInfoRepository::class
    }
