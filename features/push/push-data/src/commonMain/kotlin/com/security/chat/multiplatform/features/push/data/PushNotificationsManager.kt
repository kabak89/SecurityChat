package com.security.chat.multiplatform.features.push.data

public expect class PushNotificationsManager() {
    public fun clearNotificationsForChat(chatId: String)
}