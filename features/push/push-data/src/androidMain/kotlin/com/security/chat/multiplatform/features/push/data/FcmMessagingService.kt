package com.security.chat.multiplatform.features.push.data

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.security.chat.multiplatform.common.background.work.BackgroundWorkManager
import com.security.chat.multiplatform.common.background.work.BackgroundWorkManager.Type
import com.security.chat.multiplatform.common.background.work.BackgroundWorkPolicy
import com.security.chat.multiplatform.common.background.work.BackgroundWorkPriority
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
    private val backgroundWorkManager: BackgroundWorkManager by inject()

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
            val input = message.data
            val chatId = checkNotNull(input[ProcessNewMessageWorker.KEY_CHAT_ID])
            val legacyForeground = Type.Foreground(getString(R.string.push_message_sync_title))

            backgroundWorkManager.enqueueOneShot(
                key = "push_$chatId",
                worker = ProcessNewMessageWorker::class,
                requiresInternet = false,
                type = Type.Silent,
                policy = BackgroundWorkPolicy.APPEND_OR_REPLACE,
                priority = BackgroundWorkPriority.Expedited(legacyForeground = legacyForeground),
                input = input,
            ).invokeOnCompletion { error ->
                if (error != null) Log.e(error)
            }
        }
            .onFailure {
                Log.e(it)
            }
    }
}
