package com.security.chat.multiplatform.features.chats.ui.di

import com.security.chat.multiplatform.features.chats.ui.screens.chats.ChatsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

public val chatsUiModule: Module =
    module {
        viewModel {
            ChatsViewModel(
            )
        }
    }