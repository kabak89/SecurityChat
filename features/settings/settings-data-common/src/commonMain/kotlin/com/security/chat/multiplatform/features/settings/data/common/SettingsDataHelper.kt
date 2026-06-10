package com.security.chat.multiplatform.features.settings.data.common

import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.chats.data.storage.ChatsStorage
import com.security.chat.multiplatform.features.user.data.storage.UserStorage

public interface SettingsDataHelper {
    public suspend fun clearLocalStorages()
}

internal class SettingsDataHelperImpl(
    private val userStorage: UserStorage,
    private val chatsStorage: ChatsStorage,
    private val chatStorage: ChatStorage,
) : SettingsDataHelper {

    override suspend fun clearLocalStorages() {
        userStorage.clearAll()
        chatsStorage.clearAll()
        chatStorage.clearAll()
    }
}