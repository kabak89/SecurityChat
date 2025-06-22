package com.security.chat.multiplatform.features.chats.ui.di

import com.security.chat.multiplatform.features.chats.component.SCOPE_ID_CHATS
import com.security.chat.multiplatform.features.chats.ui.screens.addchat.AddChatViewModel
import com.security.chat.multiplatform.features.chats.ui.screens.chatlist.ChatListViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

public val chatsUiModule: Module =
    module {
        viewModel {
            ChatListViewModel(
            )
        }
        viewModel {
            AddChatViewModel(
                chatsModel = getScope(SCOPE_ID_CHATS).get(),
            )
        }
    }