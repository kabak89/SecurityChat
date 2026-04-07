package com.security.chat.multiplatform.features.chats.domain.di

import com.security.chat.multiplatform.features.chats.component.SCOPE_ID_CHATS
import com.security.chat.multiplatform.features.chats.domain.ChatsModel
import com.security.chat.multiplatform.features.chats.domain.ChatsModelImpl
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

public val chatsDomainModule: Module =
    module {
        single {
            ChatsModelImpl(
                chatsRepo = get(),
                dispatcherProvider = get(),
            )
                .apply {
                    start(parentScope = get(named(SCOPE_ID_CHATS)))
                }
        } bind ChatsModel::class
    }