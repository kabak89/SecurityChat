package com.security.chat.multiplatform.features.chats.ui.di

import com.security.chat.multiplatform.features.chats.ui.screens.addchat.AddChatViewModel
import com.security.chat.multiplatform.features.chats.ui.screens.chatlist.ChatListViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

public val chatsUiModule: Module =
    module {
        viewModelOf(::ChatListViewModel)
        viewModelOf(::AddChatViewModel)
    }