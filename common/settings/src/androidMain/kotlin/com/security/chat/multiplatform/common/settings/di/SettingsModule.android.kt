package com.security.chat.multiplatform.common.settings.di

import com.security.chat.multiplatform.common.settings.EncryptedSettings
import com.security.chat.multiplatform.common.settings.EncryptedSettingsAndroid
import com.security.chat.multiplatform.common.settings.PublicSettings
import com.security.chat.multiplatform.common.settings.PublicSettingsAndroid
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public actual val settingsModule: Module =
    module {
        singleOf(::EncryptedSettingsAndroid) bind EncryptedSettings::class
        singleOf(::PublicSettingsAndroid) bind PublicSettings::class
    }