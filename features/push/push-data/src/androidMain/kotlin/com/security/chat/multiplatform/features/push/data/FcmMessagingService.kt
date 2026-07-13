package com.security.chat.multiplatform.features.push.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.security.chat.multiplatform.common.core.component.SCOPE_ID_APP
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.push.data.storage.PushStorage
import com.security.chat.multiplatform.features.push.domain.PushRepository
import com.security.chat.multiplatform.features.push.domain.entity.NotificationInfo
import com.security.chat.multiplatform.features.push.navigation.api.IntentBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import securitychat.common.localization.generated.resources.push_channel_description_new_message
import securitychat.common.localization.generated.resources.push_channel_name_new_message

public class FcmMessagingService : FirebaseMessagingService(), KoinComponent {

    private val pushRepository: PushRepository by inject()
    private val intentBuilder: IntentBuilder by inject()
    private val pushStorage: PushStorage by inject()
    private val scope: CoroutineScope by inject(named(SCOPE_ID_APP))

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d { "FCM onNewToken: ${token.take(n = 12)}…" }

        scope.launch {
            pushRepository.onTokenRefreshed(token = token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        scope.launch {
            processNewMessage(message)
        }
    }

    private suspend fun processNewMessage(message: RemoteMessage) {
        val data = message.data

        Log.d { "new push message: $data" }

        val messageType = data["type"] ?: run {
            Log.e("no push message type. data: $data")
            return
        }

        when (messageType) {
            "chat_message" -> {
                val notificationManager: NotificationManager =
                    getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                createChannel(notificationManager)

                val chatId = requireNotNull(data["chatId"])

                if (!pushStorage.isNotificationForChatMustBeShown(chatId)) {
                    Log.d { "push message for chat $chatId must not be shown" }
                    return
                }

                val serializedMessages = requireNotNull(data["messages"])

                val notificationInfo = pushRepository.processNewMessages(
                    serializedMessages = serializedMessages,
                    chatId = chatId,
                )

                val intent = when (notificationInfo.chatType) {
                    NotificationInfo.ChatType.Personal -> {
                        intentBuilder.getOpenPersonalChatIntent(
                            context = this,
                            chatId = chatId,
                        )
                    }

                    NotificationInfo.ChatType.Group -> {
                        intentBuilder.getOpenGroupChatIntent(
                            context = this,
                            chatId = chatId,
                        )
                    }
                }

                val pendingIntent: PendingIntent = PendingIntent.getActivity(
                    /* context = */
                    this,
                    /* requestCode = */
                    chatId.hashCode(),
                    /* intent = */
                    intent,
                    /* flags = */
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )

                val builder = NotificationCompat.Builder(this, NEW_MESSAGES_CHANNEL)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(notificationInfo.title)
                    .setContentText(notificationInfo.description)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

                val notificationManagerCompat = NotificationManagerCompat.from(this)
                notificationManagerCompat.notify(
                    /* id = */
                    chatId.hashCode(),
                    /* notification = */
                    builder.build(),
                )
            }

            else -> {
                Log.e("unknown message type. type: $messageType")
                return
            }
        }
    }

    private suspend fun createChannel(notificationManager: NotificationManager) {
        val name = getString(StringRes.push_channel_name_new_message)
        val descriptionText = getString(StringRes.push_channel_description_new_message)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(
            /* id = */
            NEW_MESSAGES_CHANNEL,
            /* name = */
            name,
            /* importance = */
            importance,
        ).apply {
            description = descriptionText
        }
        notificationManager.createNotificationChannel(channel)
    }
}

private const val NEW_MESSAGES_CHANNEL = "NEW_MESSAGES_CHANNEL"
