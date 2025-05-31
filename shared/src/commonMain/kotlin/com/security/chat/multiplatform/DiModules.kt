package com.security.chat.multiplatform

import com.security.chat.multiplatform.features.authorize.ui.di.authorizeUiModule
import com.security.chat.multiplatform.features.splash.ui.di.splashUiModule

val diModules = listOf(
    authorizeUiModule,

    splashUiModule,
)