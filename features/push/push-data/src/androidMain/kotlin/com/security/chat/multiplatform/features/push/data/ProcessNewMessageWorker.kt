package com.security.chat.multiplatform.features.push.data

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.push.data.storage.PushStorage
import com.security.chat.multiplatform.features.push.domain.PushRepository
import com.security.chat.multiplatform.features.push.navigation.api.IntentBuilder
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import securitychat.common.localization.generated.resources.push_channel_description_message_sync
import securitychat.common.localization.generated.resources.push_channel_description_new_message
import securitychat.common.localization.generated.resources.push_channel_name_message_sync
import securitychat.common.localization.generated.resources.push_channel_name_new_message
import securitychat.common.localization.generated.resources.push_message_sync_title

internal class ProcessNewMessageWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val pushRepository: PushRepository by inject()
    private val intentBuilder: IntentBuilder by inject()
    private val pushStorage: PushStorage by inject()

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = createSyncNotification()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(
                /* notificationId = */
                SYNC_NOTIFICATION_ID,
                /* notification = */
                notification,
                /* foregroundServiceType = */
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE,
            )
        } else {
            ForegroundInfo(
                /* notificationId = */
                SYNC_NOTIFICATION_ID,
                /* notification = */
                notification,
            )
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        setForeground(getForegroundInfo())

        val data = inputData.keyValueMap

        Log.d { "new push message: $data" }

        val messageType = inputData.getString(KEY_TYPE) ?: run {
            Log.e("no push message type. data: $data")
            return Result.failure()
        }

        return when (messageType) {
            TYPE_CHAT_MESSAGE -> handleChatMessage()
            else -> {
                Log.e("unknown message type. type: $messageType")
                Result.failure()
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private suspend fun handleChatMessage(): Result {
        val chatId = inputData.getString(KEY_CHAT_ID) ?: run {
            Log.e("no chatId in push message")
            return Result.failure()
        }

        val notificationManager: NotificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNewMessagesChannel(notificationManager)

        if (!pushStorage.isNotificationForChatMustBeShown(chatId)) {
            Log.d { "push message for chat $chatId must not be shown" }
            return Result.success()
        }

        val serializedMessages = inputData.getString(KEY_MESSAGES) ?: run {
            Log.e("no messages in push message")
            return Result.failure()
        }

        val notificationInfo = pushRepository.processNewMessages(
            serializedMessages = serializedMessages,
            chatId = chatId,
        )

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
            .setContentTitle(notificationInfo.title)
            .setContentText(notificationInfo.description)
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

        return Result.success()
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

    private suspend fun createSyncNotification(): Notification {
        val notificationManager: NotificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            /* id = */
            MESSAGE_SYNC_CHANNEL,
            /* name = */
            getString(StringRes.push_channel_name_message_sync),
            /* importance = */
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(StringRes.push_channel_description_message_sync)
        }
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(applicationContext, MESSAGE_SYNC_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(StringRes.push_message_sync_title))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    internal companion object {
        const val KEY_TYPE: String = "type"
        const val KEY_CHAT_ID: String = "chatId"
        const val KEY_MESSAGES: String = "messages"
    }
}

private const val TYPE_CHAT_MESSAGE = "chat_message"
private const val NEW_MESSAGES_CHANNEL = "NEW_MESSAGES_CHANNEL"
private const val MESSAGE_SYNC_CHANNEL = "MESSAGE_SYNC_CHANNEL"
private const val SYNC_NOTIFICATION_ID = 1001
