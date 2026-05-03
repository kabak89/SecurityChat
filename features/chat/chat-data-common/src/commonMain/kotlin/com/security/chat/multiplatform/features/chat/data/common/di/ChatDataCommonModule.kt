package com.security.chat.multiplatform.features.chat.data.common.di

import com.security.chat.multiplatform.features.chat.data.common.ChatDataHelper
import com.security.chat.multiplatform.features.chat.data.common.ChatDataHelperImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val chatDataCommonModule: Module =
    module {
        singleOf(::ChatDataHelperImpl) bind ChatDataHelper::class
    }