package com.security.chat.multiplatform.features.push.data

import platform.UserNotifications.UNUserNotificationCenter

public actual class PushNotificationsManager {
    public actual fun clearNotificationsForChat(chatId: String) {
        UNUserNotificationCenter.currentNotificationCenter().apply {
            removePendingNotificationRequestsWithIdentifiers(listOf(chatId))
            removeDeliveredNotificationsWithIdentifiers(listOf(chatId))
        }
    }
}
