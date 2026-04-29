package com.security.chat.multiplatform.di

import com.security.chat.multiplatform.common.core.db.di.coreDbModule
import com.security.chat.multiplatform.common.core.network.di.coreNetworkModule
import com.security.chat.multiplatform.common.core.network.di.networkEngineModule
import com.security.chat.multiplatform.common.core.threading.di.coreThreadingModule
import com.security.chat.multiplatform.common.core.time.di.coreTimeModule
import com.security.chat.multiplatform.common.device.info.di.deviceInfoModule
import com.security.chat.multiplatform.common.settings.di.settingsModule
import com.security.chat.multiplatform.features.push.data.di.pushDataModule
import com.security.chat.multiplatform.features.push.domain.di.pushDomainModule
import com.security.chat.multiplatform.features.user.data.storage.di.userDataStorageModule
import org.koin.core.module.Module

internal val rootDiModules: List<Module> =
    listOf(
        sharedModule,
        coreThreadingModule,
        coreDbModule,
        coreTimeModule,
        settingsModule,
        coreNetworkModule,
        networkEngineModule,
        deviceInfoModule,

        userDataStorageModule,

        pushDataModule,
        pushDomainModule,
    )