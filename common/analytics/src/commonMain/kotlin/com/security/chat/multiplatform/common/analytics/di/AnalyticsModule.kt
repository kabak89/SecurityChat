package com.security.chat.multiplatform.common.analytics.di

import com.security.chat.multiplatform.common.analytics.Analytics
import com.security.chat.multiplatform.common.analytics.platformAnalytics
import org.koin.core.module.Module
import org.koin.dsl.module

public val analyticsModule: Module =
    module {
        single<Analytics> { platformAnalytics }
    }
