package com.security.chat.multiplatform.features.chat.data.common

import com.security.chat.multiplatform.common.background.work.BackgroundWorkManager
import com.security.chat.multiplatform.common.background.work.BackgroundWorkPolicy
import com.security.chat.multiplatform.common.background.work.BackgroundWorkPriority
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

public interface OutgoingMessageScheduler {
    public val sendDeleteMutex: Mutex

    public suspend fun saveAndSchedule(message: MessageSM)
    public suspend fun schedule(userId: String)
    public suspend fun cancel()
}

internal class OutgoingMessageSchedulerImpl(
    private val backgroundWorkManager: BackgroundWorkManager,
    private val userStorage: UserStorage,
    private val chatStorage: ChatStorage,
) : OutgoingMessageScheduler {

    override val sendDeleteMutex = Mutex()
    private val schedulingMutex = Mutex()

    override suspend fun saveAndSchedule(message: MessageSM) {
        schedulingMutex.withLock {
            check(userStorage.getUserId() == message.authorId)
            chatStorage.migrateMessageStatuses(message.authorId)
            withContext(NonCancellable) {
                chatStorage.saveMessage(message)
                enqueue(message.authorId)
            }
        }
    }

    override suspend fun schedule(userId: String) {
        schedulingMutex.withLock {
            if (userStorage.getUserId() == userId) {
                enqueue(userId)
            }
        }
    }

    override suspend fun cancel() {
        try {
            schedulingMutex.withLock {
                backgroundWorkManager.cancel(OUTGOING_MESSAGES_WORK_ID)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(error, "Could not cancel outgoing message work")
        }
        /** Wait for an active send to finish before account cleanup clears the database. */
        sendDeleteMutex.withLock { }
    }

    private suspend fun enqueue(userId: String) {
        try {
            backgroundWorkManager.enqueueOneShot(
                key = OUTGOING_MESSAGES_WORK_ID,
                worker = SendOutgoingMessagesWorker::class,
                requiresInternet = true,
                type = BackgroundWorkManager.Type.Silent,
                policy = BackgroundWorkPolicy.KEEP_PENDING,
                priority = BackgroundWorkPriority.Expedited(
                    legacyForeground = BackgroundWorkManager.Type.Silent,
                ),
                input = mapOf(OUTGOING_MESSAGES_USER_ID to userId),
            ).await()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(error, "Could not schedule outgoing messages")
        }
    }
}

internal const val OUTGOING_MESSAGES_WORK_ID = "chat.send-outgoing-messages"
internal const val OUTGOING_MESSAGES_USER_ID = "userId"
