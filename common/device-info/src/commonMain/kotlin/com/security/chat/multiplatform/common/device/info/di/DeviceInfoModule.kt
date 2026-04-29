package com.security.chat.multiplatform.common.device.info.di

import com.security.chat.multiplatform.common.device.info.DeviceInfoManager
import com.security.chat.multiplatform.common.device.info.DeviceInfoManagerImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val deviceInfoModule: Module =
    module {
        singleOf(::DeviceInfoManagerImpl) bind DeviceInfoManager::class
    }
