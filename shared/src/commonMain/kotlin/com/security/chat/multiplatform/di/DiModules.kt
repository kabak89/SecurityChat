package com.security.chat.multiplatform.di

import com.security.chat.multiplatform.common.core.db.di.coreDbModule
import com.security.chat.multiplatform.common.core.network.di.coreNetworkModule
import com.security.chat.multiplatform.common.core.network.di.networkEngineModule
import com.security.chat.multiplatform.common.core.threading.di.coreThreadingModule
import com.security.chat.multiplatform.common.core.time.di.coreTimeModule
import com.security.chat.multiplatform.common.platform.di.platformModule
import com.security.chat.multiplatform.common.settings.di.settingsModule
import org.koin.core.module.Module

val diModules: List<Module> =
    listOf(
        sharedModule,

        coreThreadingModule,
        coreDbModule,
        coreTimeModule,
        settingsModule,
        platformModule,
        coreNetworkModule,
        networkEngineModule,
    )