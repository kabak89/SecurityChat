package com.security.chat.multiplatform.features.settings.domain.di

import com.security.chat.multiplatform.features.settings.component.SCOPE_ID_SETTINGS
import com.security.chat.multiplatform.features.settings.domain.SettingsModel
import com.security.chat.multiplatform.features.settings.domain.SettingsModelImpl
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

public val settingsDomainModule: Module =
    module {
        scope(named(SCOPE_ID_SETTINGS)) {
            scoped {
                SettingsModelImpl(
                    settingsRepo = get(),
                    dispatcherProvider = get(),
                )
                    .apply {
                        start(parentScope = get())
                    }
            } bind SettingsModel::class
        }
    }