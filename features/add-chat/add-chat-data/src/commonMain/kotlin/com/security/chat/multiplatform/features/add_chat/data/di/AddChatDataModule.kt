package com.security.chat.multiplatform.features.add_chat.data.di

import com.security.chat.multiplatform.features.add_chat.data.repoimpl.AddChatRepoImpl
import com.security.chat.multiplatform.features.add_chat.domain.repo.AddChatRepo
import org.koin.core.module.Module
import org.koin.dsl.module

public val addChatDataModule: Module = module {
    factory<AddChatRepo> {
        AddChatRepoImpl(
            networkManagerFactory = get(),
            networkConfig = get(),
            userStorage = get(),
            chatsStorage = get(),
            usersStorage = get(),
            usersNetworkManager = get(),
        )
    }
}
