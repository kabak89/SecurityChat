package com.security.chat.multiplatform.features.chat_info.domain.di

import com.security.chat.multiplatform.features.chat_info.domain.AddMemberModel
import com.security.chat.multiplatform.features.chat_info.domain.AddMemberModelImpl
import com.security.chat.multiplatform.features.chat_info.domain.ChatInfoModel
import com.security.chat.multiplatform.features.chat_info.domain.ChatInfoModelImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val chatInfoDomainInfoModule: Module =
    module {
        singleOf(::ChatInfoModelImpl) bind ChatInfoModel::class
    }

public val chatInfoDomainAddMemberModule: Module =
    module {
        singleOf(::AddMemberModelImpl) bind AddMemberModel::class
    }
