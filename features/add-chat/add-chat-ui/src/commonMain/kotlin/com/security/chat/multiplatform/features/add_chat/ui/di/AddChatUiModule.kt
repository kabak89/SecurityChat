package com.security.chat.multiplatform.features.add_chat.ui.di

import com.security.chat.multiplatform.features.add_chat.ui.AddChatViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

public val addChatUiModule: Module = module {
    viewModelOf(::AddChatViewModel)
}
