package com.security.chat.multiplatform.features.settings.data.storage.di

import com.security.chat.multiplatform.common.core.component.SCOPE_ID_ROOT
import com.security.chat.multiplatform.features.settings.data.storage.SettingsStorage
import com.security.chat.multiplatform.features.settings.data.storage.SettingsStorageImpl
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

public val settingsDataStorageModule: Module =
    module {
        single {
            SettingsStorageImpl(
                publicSettings = get(),
                lifecycle = get(),
                coroutineScope = getScope(SCOPE_ID_ROOT).get(),
                dispatcherProviderInterface = get(),
            )
        } bind SettingsStorage::class
    }