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
import com.security.chat.multiplatform.features.push.navigation.impl.di.pushNavigationModule
import com.security.chat.multiplatform.features.settings.data.storage.di.settingsDataStorageModule
import com.security.chat.multiplatform.features.user.data.storage.di.userDataStorageModule
import com.security.chat.multiplatform.features.users.data.storage.di.usersDataStorageModule
import org.koin.core.module.Module

internal val commonAppDiModules: List<Module> =
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
        usersDataStorageModule,
        settingsDataStorageModule,

        pushDataModule,
        pushDomainModule,
        pushNavigationModule,
    )