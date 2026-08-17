package com.security.chat.multiplatform.features.users.data.common.di

import com.security.chat.multiplatform.features.users.data.common.UsersDataHelper
import com.security.chat.multiplatform.features.users.data.common.UsersDataHelperImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val usersDataCommonModule: Module =
    module {
        singleOf(::UsersDataHelperImpl) bind UsersDataHelper::class
    }
