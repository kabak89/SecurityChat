package com.security.chat.multiplatform.features.push.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.features.push.data.storage.PushStorage
import com.security.chat.multiplatform.features.push.domain.entity.NotificationInfo
import com.security.chat.multiplatform.features.push.navigation.api.IntentBuilder
import org.jetbrains.compose.resources.getString
import org.koin.core.module.Module
import securitychat.common.localization.generated.resources.push_channel_description_new_message
import securitychat.common.localization.generated.resources.push_channel_name_new_message

internal actual class LocalChatNotificationPresenter(
    private val applicationContext: Context,
    private val intentBuilder: IntentBuilder,
    private val pushStorage: PushStorage,
) {
    actual suspend fun show(chatId: String, info: NotificationInfo) {
        if (!pushStorage.isNotificationForChatMustBeShown(chatId) || info.messages.isEmpty() ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) !=
                    PackageManager.PERMISSION_GRANTED)
        ) {
            return
        }
        val notificationManager =
            applicationContext.getSystemService(NotificationManager::class.java)
        createNewMessagesChannel(notificationManager)
        val intent = intentBuilder.getOpenGroupChatIntent(
            context = applicationContext,
            chatId = chatId,
        )

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            /* context = */
            applicationContext,
            /* requestCode = */
            chatId.hashCode(),
            /* intent = */
            intent,
            /* flags = */
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = NotificationCompat.Builder(applicationContext, NEW_MESSAGES_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(info.title)
            .setContentText(info.description)
            .setStyle(NotificationCompat.BigTextStyle().bigText(info.description))
            .setNumber(info.messages.size)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManagerCompat = NotificationManagerCompat.from(applicationContext)
        notificationManagerCompat.notify(
            /* id = */
            chatId.hashCode(),
            /* notification = */
            builder.build(),
        )
    }

    private suspend fun createNewMessagesChannel(notificationManager: NotificationManager) {
        val name = getString(StringRes.push_channel_name_new_message)
        val descriptionText = getString(StringRes.push_channel_description_new_message)
        val channel = NotificationChannel(
            /* id = */
            NEW_MESSAGES_CHANNEL,
            /* name = */
            name,
            /* importance = */
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = descriptionText
        }
        notificationManager.createNotificationChannel(channel)
    }

}

private const val NEW_MESSAGES_CHANNEL = "NEW_MESSAGES_CHANNEL"

internal actual fun Module.registerLocalChatNotificationPresenter() {
    single {
        LocalChatNotificationPresenter(
            applicationContext = get(),
            intentBuilder = get(),
            pushStorage = get(),
        )
    }
}
