package com.security.chat.multiplatform.features.onboarding.ui.di

import com.security.chat.multiplatform.features.onboarding.ui.screens.notificationpermission.NotificationPermissionViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

public val onboardingUiModule: Module =
    module {
        viewModelOf(::NotificationPermissionViewModel)
    }
