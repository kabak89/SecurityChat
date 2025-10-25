package com.security.chat.multiplatform.features.chat.data.di

import com.security.chat.multiplatform.features.chat.component.SCOPE_ID_CHAT
import com.security.chat.multiplatform.features.chat.data.repoimpl.ChatRepoImpl
import com.security.chat.multiplatform.features.chat.domain.repo.ChatRepo
import org.koin.core.module.Module
import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

public val chatDataModule: Module =
    module {
        scope(named(SCOPE_ID_CHAT)) {
            scopedOf(::ChatRepoImpl) bind ChatRepo::class
        }
    }