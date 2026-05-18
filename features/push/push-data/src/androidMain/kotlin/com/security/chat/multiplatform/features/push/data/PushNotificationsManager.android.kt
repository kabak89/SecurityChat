package com.security.chat.multiplatform.features.push.data

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

public actual class PushNotificationsManager : KoinComponent {

    private val context: Context by inject()

    public actual fun clearNotificationsForChat(chatId: String) {
        val notificationManagerCompat = NotificationManagerCompat.from(context)
        notificationManagerCompat.cancel(
            /* id = */
            chatId.hashCode(),
        )
    }
}