package com.security.chat.multiplatform.features.chat.data.common

import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.onErr
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.chat.data.network.ChatNetworkManager
import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

public interface ChatEventsPublisher {
    public suspend fun start(chatId: String)
}

internal class ChatEventsPublisherImpl(
    private val chatStorage: ChatStorage,
    private val chatNetworkManager: ChatNetworkManager,
    private val userStorage: UserStorage,
    private val coroutineScope: CoroutineScope,
) : ChatEventsPublisher {
    private val startMutex = Mutex()
    private val restoredChats = mutableSetOf<Pair<String, String>>()
    private var publisherUserId: String? = null
    private var publisherJob: Job? = null

    override suspend fun start(chatId: String) {
        val userId = userStorage.getUserId() ?: return
        startMutex.withLock {
            val key = userId to chatId
            if (key !in restoredChats) {
                chatStorage.enqueueStoredReceivedEvents(chatId = chatId, receivedByUserId = userId)
                restoredChats += key
            }
            if (publisherUserId == userId && publisherJob?.isActive == true) return
            publisherJob?.cancel()
            publisherUserId = userId
            publisherJob = coroutineScope.launch { publishPendingEvents(userId) }
        }
    }

    private suspend fun publishPendingEvents(userId: String) {
        val retryRequired = MutableStateFlow(false)
        var observationRetryDelay = INITIAL_RETRY_DELAY
        val queueChanges = chatStorage.observePendingReceivedEvents(userId)
            .onEach { observationRetryDelay = INITIAL_RETRY_DELAY }
            .map { Unit }
            .retryWhen { error, _ ->
                Log.e(error, "Retrying delivery queue observation in $observationRetryDelay")
                delay(observationRetryDelay)
                observationRetryDelay = (observationRetryDelay * 2).coerceAtMost(MAX_RETRY_DELAY)
                true
            }
        val retryFlow: Flow<Unit> = retryRequired.flatMapLatest { required ->
            if (required) {
                flow {
                    var retryDelay = INITIAL_RETRY_DELAY
                    while (true) {
                        Log.d { "Retrying pending delivery events in $retryDelay" }
                        delay(retryDelay)
                        emit(Unit)
                        retryDelay = (retryDelay * 2).coerceAtMost(MAX_RETRY_DELAY)
                    }
                }
            } else {
                emptyFlow()
            }
        }

        merge(queueChanges, retryFlow)
            .conflate()
            .collect {
                retryRequired.value = !publishPendingBatch(userId)
            }
    }

    private suspend fun publishPendingBatch(userId: String): Boolean {
        var succeeded = true
        runSuspendCatching {
            val events = chatStorage.observePendingReceivedEvents(userId).first()
            for (event in events) {
                if (userStorage.getUserId() != userId) return@runSuspendCatching
                runSuspendCatching {
                    chatNetworkManager.publishMessageReceivedEvent(
                        eventId = event.id,
                        messageId = event.messageId,
                        chatId = event.chatId,
                        recipientUserId = event.recipientUserId,
                        receivedByUserId = event.receivedByUserId,
                    )
                    chatStorage.markReceivedEventPublished(
                        id = event.id,
                        receivedByUserId = userId,
                    )
                }
                    .onErr { error ->
                        Log.e(error, "Failed to publish delivery event: id=${event.id}")
                        succeeded = false
                    }
            }
        }
            .onErr { error ->
                Log.e(error, "Failed to process pending delivery events")
                succeeded = false
            }
        return succeeded
    }
}

private val INITIAL_RETRY_DELAY = 5.seconds
private val MAX_RETRY_DELAY = 5.minutes
