package com.security.chat.multiplatform.features.chat.ui.di

import com.security.chat.multiplatform.features.chat.ui.screens.personalchat.PersonalChatViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

public val chatUiModule: Module =
    module {
        viewModelOf(::PersonalChatViewModel)
    }