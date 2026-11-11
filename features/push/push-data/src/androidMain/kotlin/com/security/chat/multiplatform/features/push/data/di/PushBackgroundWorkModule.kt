package com.security.chat.multiplatform.features.push.data.di

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import com.security.chat.multiplatform.common.background.work.AndroidForegroundNotification
import com.security.chat.multiplatform.common.background.work.backgroundWorker
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.features.push.data.ProcessNewMessageWorker
import com.security.chat.multiplatform.features.push.data.R
import org.jetbrains.compose.resources.getString
import org.koin.core.module.Module
import org.koin.dsl.module
import securitychat.common.localization.generated.resources.push_channel_description_message_sync
import securitychat.common.localization.generated.resources.push_channel_name_message_sync

public val pushBackgroundWorkModule: Module = module {
    backgroundWorker(
        id = "push.process-new-message",
        worker = ProcessNewMessageWorker::class,
        create = {
            ProcessNewMessageWorker(
                applicationContext = get(),
                pushRepository = get(),
                intentBuilder = get(),
                pushStorage = get(),
            )
        },
        foregroundNotification = { text -> createSyncNotification(context = get(), text = text) },
    )
}

private suspend fun createSyncNotification(
    context: Context,
    text: String,
): AndroidForegroundNotification {
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    val channel = NotificationChannel(
        /* id = */ MESSAGE_SYNC_CHANNEL,
        /* name = */ getString(StringRes.push_channel_name_message_sync),
        /* importance = */ NotificationManager.IMPORTANCE_LOW,
    ).apply {
        description = getString(StringRes.push_channel_description_message_sync)
    }
    notificationManager.createNotificationChannel(channel)

    val notification = NotificationCompat.Builder(context, MESSAGE_SYNC_CHANNEL)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(text)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()
    val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
    } else {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
    }
    return AndroidForegroundNotification(
        id = SYNC_NOTIFICATION_ID,
        notification = notification,
        serviceType = serviceType,
    )
}

private const val MESSAGE_SYNC_CHANNEL = "MESSAGE_SYNC_CHANNEL"
private const val SYNC_NOTIFICATION_ID = 1001
