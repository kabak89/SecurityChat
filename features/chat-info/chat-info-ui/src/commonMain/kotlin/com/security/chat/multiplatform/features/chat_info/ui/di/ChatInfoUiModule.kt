package com.security.chat.multiplatform.features.chat_info.ui.di

import com.security.chat.multiplatform.features.chat_info.ui.screens.addmember.AddMemberViewModel
import com.security.chat.multiplatform.features.chat_info.ui.screens.chatinfo.ChatInfoViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

public val chatInfoUiModule: Module =
    module {
        viewModelOf(::ChatInfoViewModel)
        viewModelOf(::AddMemberViewModel)
    }
