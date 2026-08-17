package com.security.chat.multiplatform.features.add_chat.data.common.di

import com.security.chat.multiplatform.features.add_chat.data.common.AddChatDataHelper
import com.security.chat.multiplatform.features.add_chat.data.common.AddChatDataHelperImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val addChatDataCommonModule: Module =
    module {
        singleOf(::AddChatDataHelperImpl) bind AddChatDataHelper::class
    }
