package com.security.chat.multiplatform.features.push.data

import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.security.chat.multiplatform.common.core.component.SCOPE_ID_APP
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.push.domain.PushRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

public class FcmMessagingService : FirebaseMessagingService(), KoinComponent {

    private val pushRepository: PushRepository by inject()
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

        runCatching {
            val dataBuilder = Data.Builder()
            message.data.forEach { (key, value) -> dataBuilder.putString(key, value) }
            val inputData = dataBuilder.build()

            val request = OneTimeWorkRequestBuilder<ProcessNewMessageWorker>()
                .setInputData(inputData)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            val chatId = checkNotNull(message.data[ProcessNewMessageWorker.KEY_CHAT_ID])
            val uniqueWorkName = "push_$chatId"

            WorkManager.getInstance(applicationContext)
                .enqueueUniqueWork(
                    /* uniqueWorkName = */
                    uniqueWorkName,
                    /* existingWorkPolicy = */
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    /* request = */
                    request,
                )
        }
            .onFailure {
                Log.e(it)
            }
    }
}
