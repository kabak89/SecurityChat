package com.security.chat.multiplatform.di

import com.security.chat.multiplatform.common.core.db.di.coreDbModule
import com.security.chat.multiplatform.common.core.network.di.coreNetworkModule
import com.security.chat.multiplatform.common.core.network.di.networkEngineModule
import com.security.chat.multiplatform.common.core.threading.di.coreThreadingModule
import com.security.chat.multiplatform.common.platform.di.platformModule
import com.security.chat.multiplatform.common.settings.di.settingsModule
import com.security.chat.multiplatform.features.authorize.data.di.authorizeDataModule
import com.security.chat.multiplatform.features.authorize.domain.di.authorizeDomainModule
import com.security.chat.multiplatform.features.authorize.ui.di.authorizeUiModule
import com.security.chat.multiplatform.features.chat.data.di.chatDataModule
import com.security.chat.multiplatform.features.chat.domain.di.chatDomainModule
import com.security.chat.multiplatform.features.chat.ui.di.chatUiModule
import com.security.chat.multiplatform.features.chats.data.di.chatsDataModule
import com.security.chat.multiplatform.features.chats.data.storage.di.chatsDataStorageModule
import com.security.chat.multiplatform.features.chats.domain.di.chatsDomainModule
import com.security.chat.multiplatform.features.chats.ui.di.chatsUiModule
import com.security.chat.multiplatform.features.settings.data.di.settingsDataModule
import com.security.chat.multiplatform.features.settings.data.storage.di.settingsDataStorageModule
import com.security.chat.multiplatform.features.settings.domain.di.settingsDomainModule
import com.security.chat.multiplatform.features.settings.ui.di.settingsUiModule
import com.security.chat.multiplatform.features.splash.data.di.splashDataModule
import com.security.chat.multiplatform.features.splash.domain.di.splashDomainModule
import com.security.chat.multiplatform.features.splash.ui.di.splashUiModule
import com.security.chat.multiplatform.features.user.data.storage.di.userDataStorageModule
import com.security.chat.multiplatform.features.users.data.storage.di.usersDataStorageModule
import org.koin.core.module.Module

val diModules: List<Module> =
    listOf(
        sharedModule,

        coreThreadingModule,
        coreDbModule,
        settingsModule,
        platformModule,
        coreNetworkModule,
        networkEngineModule,

        authorizeUiModule,
        authorizeDomainModule,
        authorizeDataModule,

        splashDomainModule,
        splashUiModule,
        splashDataModule,

        userDataStorageModule,

        chatsUiModule,
        chatsDomainModule,
        chatsDataModule,
        chatsDataStorageModule,

        chatUiModule,
        chatDomainModule,
        chatDataModule,

        usersDataStorageModule,

        settingsUiModule,
        settingsDomainModule,
        settingsDataModule,
        settingsDataStorageModule,
    )