package com.security.chat.multiplatform.features.chat.data.common.di

import com.security.chat.multiplatform.common.core.component.SCOPE_ID_APP
import com.security.chat.multiplatform.features.chat.data.common.ChatDataHelper
import com.security.chat.multiplatform.features.chat.data.common.ChatDataHelperImpl
import com.security.chat.multiplatform.features.chat.data.common.ChatEventsPublisher
import com.security.chat.multiplatform.features.chat.data.common.ChatEventsPublisherImpl
import com.security.chat.multiplatform.features.chat.data.common.ChatPresenceHelper
import com.security.chat.multiplatform.features.chat.data.common.ChatPresenceHelperImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

public val chatDataCommonModule: Module =
    module {
        singleOf(::ChatDataHelperImpl) bind ChatDataHelper::class
        singleOf(::ChatPresenceHelperImpl) bind ChatPresenceHelper::class
        single<ChatEventsPublisher> {
            ChatEventsPublisherImpl(
                chatStorage = get(),
                chatNetworkManager = get(),
                userStorage = get(),
                coroutineScope = get(named(SCOPE_ID_APP)),
            )
        }
    }
