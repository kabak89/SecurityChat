package com.security.chat.multiplatform.features.push.data

import com.security.chat.multiplatform.features.push.domain.entity.NotificationInfo
import org.koin.core.module.Module
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

internal actual class LocalChatNotificationPresenter {
    actual suspend fun show(chatId: String, info: NotificationInfo) {
        val content = UNMutableNotificationContent().apply {
            setTitle(info.title)
            setBody(info.description)
            setSound(UNNotificationSound.defaultSound)
            setUserInfo(mapOf("chatId" to chatId))
        }
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = chatId,
            content = content,
            trigger = null,
        )
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request, null)
    }
}

internal actual fun Module.registerLocalChatNotificationPresenter() {
    single { LocalChatNotificationPresenter() }
}
