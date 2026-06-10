package com.security.chat.multiplatform.features.settings.data.common.di

import com.security.chat.multiplatform.features.settings.data.common.SettingsDataHelper
import com.security.chat.multiplatform.features.settings.data.common.SettingsDataHelperImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val settingsDataCommonModule: Module =
    module {
        singleOf(::SettingsDataHelperImpl) bind SettingsDataHelper::class
    }