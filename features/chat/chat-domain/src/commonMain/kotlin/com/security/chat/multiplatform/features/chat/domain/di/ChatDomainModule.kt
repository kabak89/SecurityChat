package com.security.chat.multiplatform.features.chat.domain.di

import com.security.chat.multiplatform.features.chat.domain.GroupChatModel
import com.security.chat.multiplatform.features.chat.domain.GroupChatModelImpl
import com.security.chat.multiplatform.features.chat.domain.PersonalChatModel
import com.security.chat.multiplatform.features.chat.domain.PersonalChatModelImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val chatDomainModule: Module =
    module {
        singleOf(::PersonalChatModelImpl) bind PersonalChatModel::class
        singleOf(::GroupChatModelImpl) bind GroupChatModel::class
    }