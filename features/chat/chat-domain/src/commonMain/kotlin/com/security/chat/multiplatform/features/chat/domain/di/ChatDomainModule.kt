package com.security.chat.multiplatform.features.chat.domain.di

import com.security.chat.multiplatform.features.chat.domain.GroupChatModel
import com.security.chat.multiplatform.features.chat.domain.GroupChatModelImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val chatDomainModule: Module =
    module {
        singleOf(::GroupChatModelImpl) bind GroupChatModel::class
    }