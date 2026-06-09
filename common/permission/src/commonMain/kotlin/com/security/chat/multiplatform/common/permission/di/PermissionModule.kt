package com.security.chat.multiplatform.common.permission.di

import com.security.chat.multiplatform.common.permission.PermissionsManager
import com.security.chat.multiplatform.common.permission.PermissionsManagerImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val permissionModule: Module =
    module {
        singleOf(::PermissionsManagerImpl) bind PermissionsManager::class
    }
