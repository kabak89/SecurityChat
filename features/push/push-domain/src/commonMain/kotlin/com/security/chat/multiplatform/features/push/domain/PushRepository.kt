package com.security.chat.multiplatform.features.push.domain

import com.security.chat.multiplatform.features.push.domain.entity.NotificationInfo

public interface PushRepository {
    public suspend fun registerCurrentToken()
    public suspend fun onTokenRefreshed(token: String)
    public suspend fun getInterlocutorName(chatId: String): String?

    public suspend fun processNewMessages(
        serializedMessages: String,
        chatId: String,
    ): NotificationInfo

    public fun setShowNotificationsForChat(chatId: String, show: Boolean)
    public fun isNotificationForChatMustBeShown(chatId: String): Boolean
    public fun clearNotificationsForChat(chatId: String)
}