package com.security.chat.multiplatform.features.push.data.storage

public interface PushStorage {
    public fun setShowNotificationsForChat(chatId: String, show: Boolean)
    public fun isNotificationForChatMustBeShown(chatId: String): Boolean
}

internal class PushStorageImpl : PushStorage {

    private val showNotificationsForChatMap: MutableMap<String, Boolean> = mutableMapOf()

    override fun setShowNotificationsForChat(chatId: String, show: Boolean) {
        showNotificationsForChatMap[chatId] = show
    }

    override fun isNotificationForChatMustBeShown(chatId: String): Boolean {
        return showNotificationsForChatMap[chatId] ?: true
    }
}