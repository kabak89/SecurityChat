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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
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

        return incomingFlow
            .filter { it.id == id }
            .map { json.decodeFromString<Event>(it.payload) }
            .onStart {
                activeSubscriptions.update { it + (id to subscribeMessageJson) }
                coroutineScope.launch { startSocketLoopIfNeeded() }
            }
            .onCompletion {
                activeSubscriptions.update { it - id }
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
            if (activeSubscriptions.value.isEmpty()) {
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

            if (activeSubscriptions.value.isEmpty()) {
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
                val sent: MutableSet<String> = mutableSetOf()
                val syncJob = activeSubscriptions
                    .onEach { current ->
                        for (newId in current.keys - sent) {
                            val message = current[newId] ?: continue
                            Log.d { "subscribe message = $message" }
                            send(Frame.Text(message))
                            sent.add(newId)
                        }
                        for (goneId in sent - current.keys) {
                            val unsubscribe = SocketSubscribeMessage(
                                id = goneId,
                                type = UNSUBSCRIBE_MESSAGE_TYPE,
                                payload = goneId,
                            )
                            val unsubscribeText = json.encodeToString(unsubscribe)
                            Log.d { "unsubscribe message = $unsubscribeText" }
                            send(Frame.Text(unsubscribeText))
                            sent.remove(goneId)
                        }
                    }
                    .launchIn(this)

                val closeGuardJob = launch {
                    merge(
                        connectivityObserver.isOnline
                            .filter { !it }
                            .map { CloseReason(CloseReason.Codes.GOING_AWAY, "offline") },
                        activeSubscriptions
                            .filter { it.isEmpty() }
                            .map { CloseReason(CloseReason.Codes.NORMAL, "no subscribers") },
                    ).first().let { reason ->
                        Log.d { "closing socket: ${reason.message}" }
                        close(reason)
                    }
                }

                try {
                    for (receivedFrame in incoming) {
                        val textFrame = (receivedFrame as? Frame.Text)
                            ?: error("Wrong message type: $receivedFrame")
                        val newMessage: SocketMessage =
                            json.decodeFromString(textFrame.readText())
                        incomingFlow.emit(newMessage)
                    }
                } finally {
                    syncJob.cancel()
                    closeGuardJob.cancel()
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
