package com.security.chat.multiplatform

import com.security.chat.multiplatform.common.core.db.di.coreDbModule
import com.security.chat.multiplatform.common.core.network.di.coreNetworkModule
import com.security.chat.multiplatform.common.core.network.di.networkEngineModule
import com.security.chat.multiplatform.common.core.threading.di.coreThreadingModule
import com.security.chat.multiplatform.common.platform.di.platformModule
import com.security.chat.multiplatform.common.settings.di.settingsModule
import com.security.chat.multiplatform.features.authorize.data.di.authorizeDataModule
import com.security.chat.multiplatform.features.authorize.domain.di.authorizeDomainModule
import com.security.chat.multiplatform.features.authorize.ui.di.authorizeUiModule
import com.security.chat.multiplatform.features.chat.domain.di.chatDomainModule
import com.security.chat.multiplatform.features.chat.ui.di.chatUiModule
import com.security.chat.multiplatform.features.chats.data.di.chatsDataModule
import com.security.chat.multiplatform.features.chats.domain.di.chatsDomainModule
import com.security.chat.multiplatform.features.chats.ui.di.chatsUiModule
import com.security.chat.multiplatform.features.splash.data.di.splashDataModule
import com.security.chat.multiplatform.features.splash.domain.di.splashDomainModule
import com.security.chat.multiplatform.features.splash.ui.di.splashUiModule
import com.security.chat.multiplatform.features.user.data.storage.di.userDataStorageModule
import org.koin.core.module.Module

val diModules: List<Module> =
    listOf(
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

        chatUiModule,
        chatDomainModule,
    )