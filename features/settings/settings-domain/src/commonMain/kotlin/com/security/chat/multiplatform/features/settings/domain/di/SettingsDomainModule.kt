package com.security.chat.multiplatform.features.settings.domain.di

import com.security.chat.multiplatform.features.settings.domain.SettingsModel
import com.security.chat.multiplatform.features.settings.domain.SettingsModelImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val settingsDomainModule: Module =
    module {
        singleOf(::SettingsModelImpl) bind SettingsModel::class
    }