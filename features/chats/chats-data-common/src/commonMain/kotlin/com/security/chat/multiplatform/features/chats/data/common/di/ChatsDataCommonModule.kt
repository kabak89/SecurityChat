package com.security.chat.multiplatform.features.chats.data.common.di

import com.security.chat.multiplatform.features.chats.data.common.ChatsDataHelper
import com.security.chat.multiplatform.features.chats.data.common.ChatsDataHelperImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val chatsDataCommonModule: Module =
    module {
        singleOf(::ChatsDataHelperImpl) bind ChatsDataHelper::class
    }
