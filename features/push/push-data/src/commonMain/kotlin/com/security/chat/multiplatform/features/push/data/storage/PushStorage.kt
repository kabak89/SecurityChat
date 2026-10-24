package com.security.chat.multiplatform.features.push.data.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

public interface PushStorage {
    public fun setShowNotificationsForChat(chatId: String, show: Boolean)
    public fun isNotificationForChatMustBeShown(chatId: String): Boolean
}

internal class PushStorageImpl : PushStorage {

    private val suppressedChats: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())

    override fun setShowNotificationsForChat(chatId: String, show: Boolean) {
        suppressedChats.update { chats ->
            if (show) chats - chatId else chats + chatId
        }
    }

    override fun isNotificationForChatMustBeShown(chatId: String): Boolean {
        return chatId !in suppressedChats.value
    }
}
