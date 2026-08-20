package com.security.chat.multiplatform.di

import com.security.chat.multiplatform.common.analytics.di.analyticsModule
import com.security.chat.multiplatform.common.core.db.di.coreDbModule
import com.security.chat.multiplatform.common.core.files.di.coreFilesModule
import com.security.chat.multiplatform.common.core.network.di.coreNetworkModule
import com.security.chat.multiplatform.common.core.network.di.networkEngineModule
import com.security.chat.multiplatform.common.core.threading.di.coreThreadingModule
import com.security.chat.multiplatform.common.core.time.di.coreTimeModule
import com.security.chat.multiplatform.common.crash.report.di.crashReportModule
import com.security.chat.multiplatform.common.device.info.di.deviceInfoModule
import com.security.chat.multiplatform.common.permission.di.permissionModule
import com.security.chat.multiplatform.common.settings.di.settingsModule
import com.security.chat.multiplatform.features.add_chat.data.common.di.addChatDataCommonModule
import com.security.chat.multiplatform.features.chat.data.common.di.chatDataCommonModule
import com.security.chat.multiplatform.features.chat.data.network.di.chatDataNetworkModule
import com.security.chat.multiplatform.features.chat.data.storage.di.chatDataStorageModule
import com.security.chat.multiplatform.features.chats.data.common.di.chatsDataCommonModule
import com.security.chat.multiplatform.features.chats.data.storage.di.chatsDataStorageModule
import com.security.chat.multiplatform.features.push.data.di.pushDataModule
import com.security.chat.multiplatform.features.push.domain.di.pushDomainModule
import com.security.chat.multiplatform.features.push.navigation.impl.di.pushNavigationModule
import com.security.chat.multiplatform.features.settings.data.common.di.settingsDataCommonModule
import com.security.chat.multiplatform.features.settings.data.storage.di.settingsDataStorageModule
import com.security.chat.multiplatform.features.user.data.storage.di.userDataStorageModule
import com.security.chat.multiplatform.features.users.data.common.di.usersDataCommonModule
import com.security.chat.multiplatform.features.users.data.network.di.usersNetworkManager
import com.security.chat.multiplatform.features.users.data.storage.di.usersDataStorageModule
import org.koin.core.module.Module

internal val commonAppDiModules: List<Module> =
    listOf(
        sharedModule,
        crashReportModule,
        analyticsModule,
        coreThreadingModule,
        coreDbModule,
        coreFilesModule,
        coreTimeModule,
        settingsModule,
        coreNetworkModule,
        networkEngineModule,
        deviceInfoModule,
        permissionModule,

        addChatDataCommonModule,

        chatsDataCommonModule,

        usersDataCommonModule,
        usersNetworkManager,

        chatDataCommonModule,

        userDataStorageModule,
        usersDataStorageModule,
        chatsDataStorageModule,
        chatDataStorageModule,

        chatDataNetworkModule,

        pushDataModule,
        pushDomainModule,
        pushNavigationModule,

        settingsDataStorageModule,
        settingsDataCommonModule,
    )