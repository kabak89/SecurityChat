package com.security.chat.multiplatform.common.core.network

import com.security.chat.multiplatform.common.core.network.entity.SocketConfig
import com.security.chat.multiplatform.common.core.network.entity.SocketMessage
import com.security.chat.multiplatform.common.core.network.entity.SocketSubscribeMessage
import com.security.chat.multiplatform.common.log.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

public class LiveEventsManager(
    @PublishedApi
    internal val json: Json,
    @PublishedApi
    internal val coroutineScope: CoroutineScope,
    private val socketConfig: SocketConfig,
    private val connectivityObserver: ConnectivityObserver,
    httpClientFactory: HttpClientFactory,
) {

    private val httpClient: HttpClient = httpClientFactory.build()
    private val loopMutex = Mutex()
    private var reconnectJob: Job? = null

    @PublishedApi
    internal val incomingFlow: MutableSharedFlow<SocketMessage> = MutableSharedFlow()

    @PublishedApi
    internal val messagesToSendFlow: Channel<String> = Channel(Channel.UNLIMITED)

    @PublishedApi
    internal val subscribersCounter: MutableStateFlow<Int> = MutableStateFlow(0)

    @PublishedApi
    internal val activeSubscriptions: MutableStateFlow<Map<String, String>> =
        MutableStateFlow(emptyMap())

    @OptIn(ExperimentalUuidApi::class)
    public inline fun <reified Event, reified SubscribeMessage> subscribe(
        subscribeMessage: SubscribeMessage,
        type: String,
    ): Flow<Event> {
        val id = Uuid.random().toString()
        val socketSubscribeMessage = SocketSubscribeMessage(
            id = id,
            type = type,
            payload = json.encodeToString(subscribeMessage),
        )
        val subscribeMessageJson = json.encodeToString(socketSubscribeMessage)

        activeSubscriptions.update { it + (id to subscribeMessageJson) }
        subscribersCounter.update { it + 1 }

        coroutineScope.launch {
            startSocketLoopIfNeeded()
        }

        return callbackFlow {
            incomingFlow
                .mapNotNull { socketMessage ->
                    if (socketMessage.id != id) return@mapNotNull null
                    json.decodeFromString<Event>(socketMessage.payload)
                }
                .onEach {
                    trySendBlocking(it)
                }
                .launchIn(this)

            awaitClose {
                activeSubscriptions.update { it - id }

                val socketUnsubscribeMessage = SocketSubscribeMessage(
                    id = id,
                    type = UNSUBSCRIBE_MESSAGE_TYPE,
                    payload = id,
                )
                val unsubscribeMessageText = json.encodeToString(socketUnsubscribeMessage)
                messagesToSendFlow.trySend(unsubscribeMessageText)
                subscribersCounter.update { it - 1 }
            }
        }
    }

    @PublishedApi
    internal suspend fun startSocketLoopIfNeeded() {
        loopMutex.withLock {
            val current = reconnectJob
            if (current != null && current.isActive) return
            reconnectJob = coroutineScope.launch { runReconnectLoop() }
        }
    }

    private suspend fun runReconnectLoop() {
        var attempt = 0
        while (true) {
            if (subscribersCounter.value == 0) {
                Log.d { "reconnect loop: no subscribers, exit" }
                return
            }

            Log.d { "reconnect loop: waiting for network" }
            connectivityObserver.isOnline.first { it }

            val gracefulEnd = try {
                runWebSocketSession()
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(e, "websocket session failed")
                false
            }

            if (subscribersCounter.value == 0) {
                Log.d { "reconnect loop: subscribers drained, exit" }
                return
            }

            if (gracefulEnd) {
                // Session closed due to network drop or server close — retry
                // straight away once we are online again.
                attempt = 0
            } else {
                val delayMs = reconnectBackoffMs(attempt)
                attempt++
                Log.d { "reconnect loop: backoff ${delayMs}ms before retry" }
                delay(delayMs)
            }
        }
    }

    private suspend fun runWebSocketSession() {
        httpClient.webSocket(
            host = socketConfig.host,
            port = socketConfig.port,
            path = socketConfig.path,
            block = {
                val outgoingMessagesJob = launch {
                    for (message in messagesToSendFlow) {
                        Log.d { "outgoing message = $message" }
                        send(Frame.Text(message))
                    }
                }

                val sentSubscriptions: MutableSet<String> = mutableSetOf()
                val replayJob = activeSubscriptions
                    .onEach { current ->
                        val newIds = current.keys - sentSubscriptions
                        for (newId in newIds) {
                            val message = current[newId] ?: continue
                            Log.d { "subscribe message = $message" }
                            send(Frame.Text(message))
                            sentSubscriptions.add(newId)
                        }
                    }
                    .launchIn(this)

                val watchdogJob = launch {
                    connectivityObserver.isOnline.filter { !it }.first()
                    Log.d { "watchdog: offline detected, closing socket" }
                    close(CloseReason(CloseReason.Codes.GOING_AWAY, "offline"))
                }

                val noSubscribersJob = subscribersCounter
                    .onEach { subscribersCount ->
                        Log.d { "subscribersCount = $subscribersCount" }
                        if (subscribersCount == 0) {
                            Log.d { "no subscribers, closing socket" }
                            close(CloseReason(CloseReason.Codes.NORMAL, "no subscribers"))
                        }
                    }
                    .launchIn(this)

                try {
                    for (receivedFrame in incoming) {
                        val textFrame = (receivedFrame as? Frame.Text)
                            ?: error("Wrong message type: $receivedFrame")
                        val newMessage: SocketMessage =
                            json.decodeFromString(textFrame.readText())
                        incomingFlow.emit(newMessage)
                    }
                } finally {
                    outgoingMessagesJob.cancel()
                    replayJob.cancel()
                    watchdogJob.cancel()
                    noSubscribersJob.cancel()
                }
            },
        )
    }

    private fun reconnectBackoffMs(attempt: Int): Long {
        val bounded = attempt.coerceIn(0, BACKOFF_MAX_SHIFTS)
        return (BACKOFF_BASE_MS shl bounded).coerceAtMost(BACKOFF_MAX_MS)
    }

}

@PublishedApi
internal const val UNSUBSCRIBE_MESSAGE_TYPE: String = "unsubscribe"

private const val BACKOFF_BASE_MS: Long = 500L
private const val BACKOFF_MAX_MS: Long = 30_000L
private const val BACKOFF_MAX_SHIFTS: Int = 6
