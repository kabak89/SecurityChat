package com.security.chat.multiplatform.features.chats.data.di

import com.security.chat.multiplatform.features.chats.component.SCOPE_ID_CHATS
import com.security.chat.multiplatform.features.chats.data.repoimpl.ChatsRepoImpl
import com.security.chat.multiplatform.features.chats.domain.repo.ChatsRepo
import org.koin.core.module.Module
import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

public val chatsDataModule: Module =
    module {
        scope(named(SCOPE_ID_CHATS)) {
            scopedOf(::ChatsRepoImpl) bind ChatsRepo::class
        }
    }