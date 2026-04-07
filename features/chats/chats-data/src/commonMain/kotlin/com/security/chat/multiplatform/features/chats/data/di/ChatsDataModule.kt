package com.security.chat.multiplatform.features.chats.data.di

import com.security.chat.multiplatform.features.chats.data.repoimpl.ChatsRepoImpl
import com.security.chat.multiplatform.features.chats.domain.repo.ChatsRepo
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val chatsDataModule: Module =
    module {
        singleOf(::ChatsRepoImpl) bind ChatsRepo::class
    }