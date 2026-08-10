package com.security.chat.multiplatform.features.add_chat.domain.di

import com.security.chat.multiplatform.features.add_chat.domain.CreateChatModel
import com.security.chat.multiplatform.features.add_chat.domain.CreateChatModelImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val addChatDomainModule: Module =
    module {
        singleOf(::CreateChatModelImpl) bind CreateChatModel::class
    }
