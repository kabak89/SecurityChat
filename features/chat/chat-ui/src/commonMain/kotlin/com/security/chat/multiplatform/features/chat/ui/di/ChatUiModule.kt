package com.security.chat.multiplatform.features.chat.ui.di

import com.security.chat.multiplatform.features.chat.component.SCOPE_ID_CHAT
import com.security.chat.multiplatform.features.chat.ui.screens.personalchat.PersonalChatViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

public val chatUiModule: Module =
    module {
        viewModel {
            PersonalChatViewModel(
                chatModel = getScope(SCOPE_ID_CHAT).get(),
                params = get(),
            )
        }
    }