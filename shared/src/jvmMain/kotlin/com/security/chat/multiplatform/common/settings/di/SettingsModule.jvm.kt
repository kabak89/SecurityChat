package com.security.chat.multiplatform.common.settings.di

import com.security.chat.multiplatform.common.settings.EncryptedSettings
import com.security.chat.multiplatform.common.settings.EncryptedSettingsDesktop
import com.security.chat.multiplatform.common.settings.PublicSettings
import com.security.chat.multiplatform.common.settings.PublicSettingsDesktop
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val settingsModule: Module =
    module {
        singleOf(::EncryptedSettingsDesktop) bind EncryptedSettings::class
        singleOf(::PublicSettingsDesktop) bind PublicSettings::class
    }