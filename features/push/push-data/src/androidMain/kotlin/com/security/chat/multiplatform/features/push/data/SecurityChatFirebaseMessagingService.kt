package com.security.chat.multiplatform.features.push.data

import android.Manifest
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.push.domain.PushRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

public class SecurityChatFirebaseMessagingService : FirebaseMessagingService(), KoinComponent {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pushRepository: PushRepository by inject()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d { "FCM onNewToken: ${token.take(n = 12)}…" }

        scope.launch {
            pushRepository.onTokenRefreshed(token = token)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data

        Log.d { "new push message: $data" }

        val messageType = data["type"] ?: run {
            Log.e("no push message type. data: $data")
            return
        }

        when (messageType) {
            "chat_message" -> {
                //TODO localize
                val name = "New messages"
                val descriptionText = "Channel for new chat messages"
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

                val notificationManager: NotificationManager =
                    getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)

                val builder = NotificationCompat.Builder(this, NEW_MESSAGES_CHANNEL)
                    .setSmallIcon(R.drawable.ic_secure)
                    .setContentTitle("New message")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setAutoCancel(true)

                val notificationManagerCompat = NotificationManagerCompat.from(this)
                // notificationId — уникальное число для каждого уведомления
                notificationManagerCompat.notify(101, builder.build())
            }

            else -> {
                Log.e("unknown message type. type: $messageType")
                return
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

private const val NEW_MESSAGES_CHANNEL = "NEW_MESSAGES_CHANNEL"
