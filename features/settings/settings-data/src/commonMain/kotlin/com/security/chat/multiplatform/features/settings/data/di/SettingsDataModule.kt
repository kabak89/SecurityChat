package com.security.chat.multiplatform.features.settings.data.di

import com.security.chat.multiplatform.features.settings.data.SettingsRepoImpl
import com.security.chat.multiplatform.features.settings.domain.repo.SettingsRepo
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val settingsDataModule: Module =
    module {
        singleOf(::SettingsRepoImpl) bind SettingsRepo::class
    }