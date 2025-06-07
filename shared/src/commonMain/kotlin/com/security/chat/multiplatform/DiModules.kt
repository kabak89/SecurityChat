package com.security.chat.multiplatform

import com.security.chat.multiplatform.common.core.db.di.coreDbModule
import com.security.chat.multiplatform.common.core.threading.di.coreThreadingModule
import com.security.chat.multiplatform.common.platform.di.platformModule
import com.security.chat.multiplatform.common.settings.di.settingsModule
import com.security.chat.multiplatform.features.authorize.domain.di.signInDomainModule
import com.security.chat.multiplatform.features.authorize.ui.di.authorizeUiModule
import com.security.chat.multiplatform.features.splash.data.di.splashDataModule
import com.security.chat.multiplatform.features.splash.domain.di.splashDomainModule
import com.security.chat.multiplatform.features.splash.ui.di.splashUiModule
import com.security.chat.multiplatform.features.user.data_storage.di.userDataStorageModule

val diModules = listOf(
    coreThreadingModule,
    coreDbModule,
    settingsModule,
    platformModule,

    authorizeUiModule,

    signInDomainModule,

    splashDomainModule,
    splashUiModule,
    splashDataModule,

    userDataStorageModule,
)