package com.security.chat.multiplatform.features.push.data

import com.security.chat.multiplatform.features.push.domain.entity.NotificationInfo
import org.koin.core.module.Module
import java.awt.EventQueue
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import javax.swing.UIManager

internal actual class LocalChatNotificationPresenter {

    private val icon by lazy {
        val image = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            UIManager.getIcon("OptionPane.informationIcon").paintIcon(null, graphics, 0, 0)
        } finally {
            graphics.dispose()
        }
        TrayIcon(image, "SecurityChat").apply { isImageAutoSize = true }
    }

    actual suspend fun show(chatId: String, info: NotificationInfo) {
        if (!SystemTray.isSupported()) return
        EventQueue.invokeLater {
            val tray = SystemTray.getSystemTray()
            if (icon !in tray.trayIcons) {
                try {
                    tray.add(icon)
                } catch (_: java.awt.AWTException) {
                    return@invokeLater
                }
            }
            icon.displayMessage(info.title, info.description, TrayIcon.MessageType.INFO)
        }
    }
}

internal actual fun Module.registerLocalChatNotificationPresenter() {
    single { LocalChatNotificationPresenter() }
}
