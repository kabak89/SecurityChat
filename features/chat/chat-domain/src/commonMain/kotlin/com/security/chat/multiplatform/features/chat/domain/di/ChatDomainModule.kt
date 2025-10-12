package com.security.chat.multiplatform.features.chat.domain.di

import com.security.chat.multiplatform.features.chat.component.SCOPE_ID_CHAT
import com.security.chat.multiplatform.features.chat.domain.ChatModel
import com.security.chat.multiplatform.features.chat.domain.ChatModelImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

public val chatDomainModule: Module =
    module {
        scope(named(SCOPE_ID_CHAT)) {
            scopedOf(::ChatModelImpl) bind ChatModel::class
        }
    }