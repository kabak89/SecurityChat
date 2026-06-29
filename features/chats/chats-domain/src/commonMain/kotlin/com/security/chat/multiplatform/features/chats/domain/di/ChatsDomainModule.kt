package com.security.chat.multiplatform.features.chats.domain.di

import com.security.chat.multiplatform.features.chats.domain.ChatsModel
import com.security.chat.multiplatform.features.chats.domain.ChatsModelImpl
import com.security.chat.multiplatform.features.chats.domain.CreateChatModel
import com.security.chat.multiplatform.features.chats.domain.CreateChatModelImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val chatsDomainModule: Module =
    module {
        singleOf(::ChatsModelImpl) bind ChatsModel::class
        singleOf(::CreateChatModelImpl) bind CreateChatModel::class
    }