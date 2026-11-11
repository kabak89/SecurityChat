package com.security.chat.multiplatform.features.push.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.security.chat.multiplatform.common.background.work.BackgroundWorker
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.push.data.storage.PushStorage
import com.security.chat.multiplatform.features.push.domain.PushRepository
import com.security.chat.multiplatform.features.push.navigation.api.IntentBuilder
import org.jetbrains.compose.resources.getString
import securitychat.common.localization.generated.resources.push_channel_description_new_message
import securitychat.common.localization.generated.resources.push_channel_name_new_message

internal class ProcessNewMessageWorker(
    private val applicationContext: Context,
    private val pushRepository: PushRepository,
    private val intentBuilder: IntentBuilder,
    private val pushStorage: PushStorage,
) : BackgroundWorker {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun run(input: Map<String, String>) {
        Log.d { "new push message: $input" }

        val messageType = checkNotNull(input[KEY_TYPE]) { "No push message type" }
        when (messageType) {
            TYPE_CHAT_MESSAGE -> handleChatMessage(input)
            else -> error("Unknown push message type: $messageType")
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private suspend fun handleChatMessage(input: Map<String, String>) {
        val chatId = checkNotNull(input[KEY_CHAT_ID]) { "No chatId in push message" }

        val notificationManager: NotificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNewMessagesChannel(notificationManager)

        if (!pushStorage.isNotificationForChatMustBeShown(chatId)) {
            Log.d { "Skipped notification for active chat: chatId=$chatId" }
            return
        }

        val serializedMessages = checkNotNull(input[KEY_MESSAGES]) { "No messages in push message" }

        val notificationInfo = pushRepository.processNewMessages(
            serializedMessages = serializedMessages,
            chatId = chatId,
        )

        if (
            notificationInfo.messages.isEmpty() ||
            !pushStorage.isNotificationForChatMustBeShown(chatId)
        ) {
            pushRepository.clearNotificationsForChat(chatId)
            return
        }

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
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationInfo.description))
            .setNumber(notificationInfo.messages.size)
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

    internal companion object {
        const val KEY_TYPE: String = "type"
        const val KEY_CHAT_ID: String = "chatId"
        const val KEY_MESSAGES: String = "messages"
    }
}

private const val TYPE_CHAT_MESSAGE = "chat_message"
private const val NEW_MESSAGES_CHANNEL = "NEW_MESSAGES_CHANNEL"
