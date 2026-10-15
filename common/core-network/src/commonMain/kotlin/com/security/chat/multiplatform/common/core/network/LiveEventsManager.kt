package com.security.chat.multiplatform.common.core.network

import com.security.chat.multiplatform.common.core.network.entity.SOCKET_AUTH_ID
import com.security.chat.multiplatform.common.core.network.entity.SocketConfig
import com.security.chat.multiplatform.common.core.network.entity.SocketEvent
import com.security.chat.multiplatform.common.core.network.entity.SocketMessage
import com.security.chat.multiplatform.common.core.network.entity.SocketSendMessage
import com.security.chat.multiplatform.common.core.network.entity.SocketSubscribeMessage
import com.security.chat.multiplatform.common.log.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.clearAuthTokens
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.URLProtocol
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlin.uuid.Uuid

public class LiveEventsManager(
    @PublishedApi
    internal val json: Json,
    @PublishedApi
    internal val coroutineScope: CoroutineScope,
    private val socketConfig: SocketConfig,
    private val connectivityObserver: ConnectivityObserver,
    httpClientFactory: HttpClientFactory,
    private val tokenManager: TokenManager,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {

    private val httpClient: HttpClient = httpClientFactory.build(needAuthorization = true)
    private val loopMutex = Mutex()
    private var reconnectJob: Job? = null
    private val activeSession = MutableStateFlow<Connection?>(null)
    private var nextConnectionId = 0L

    @PublishedApi
    internal val incomingFlow: MutableSharedFlow<ReceivedMessage> = MutableSharedFlow()

    @PublishedApi
    internal val activeSubscriptions: MutableStateFlow<Map<String, String>> =
        MutableStateFlow(emptyMap())

    /**
     * Sends a typed payload through the connection maintained by active subscriptions.
     * Fails when disconnected and does not retry or wait for a server acknowledgement.
     */
    public suspend inline fun <reified Message> send(
        message: Message,
        type: String,
        connectionId: Long? = null,
    ) {
        val socketMessage = SocketSendMessage(
            type = type,
            payload = json.encodeToString(message),
        )
        sendSerialized(json.encodeToString(socketMessage), connectionId)
    }

    public fun getConnectionIdFlow(): Flow<Long?> = activeSession.map { it?.id }

    public inline fun <reified Event, reified SubscribeMessage> subscribe(
        subscribeMessage: SubscribeMessage,
        type: String,
    ): Flow<Event> =
        subscribeEvents<Event, SubscribeMessage>(subscribeMessage, type)
            .filterIsInstance<SocketEvent.Message<Event>>()
            .map { it.value }

    /** Associates messages with their transport session and invalidates snapshots on disconnect. */
    public inline fun <reified Event, reified SubscribeMessage> subscribeEvents(
        subscribeMessage: SubscribeMessage,
        type: String,
    ): Flow<SocketEvent<Event>> =
        subscribeSerialized(json.encodeToString(subscribeMessage), type)
            .map<ReceivedMessage, SocketEvent<Event>> { received ->
                val message = received.message
                if (message == null) {
                    SocketEvent.Disconnected
                } else {
                    SocketEvent.Message(
                        received.connectionId,
                        json.decodeFromString<Event>(message.payload),
                    )
                }
            }
            .onStart { emit(SocketEvent.Disconnected) }

    @PublishedApi
    internal fun subscribeSerialized(payload: String, type: String): Flow<ReceivedMessage> {
        val id = Uuid.random().toString()
        val message = json.encodeToString(SocketSubscribeMessage(id, type, payload))
        return incomingFlow
            .onSubscription {
                activeSubscriptions.update { it + (id to message) }
                coroutineScope.launch { startSocketLoopIfNeeded() }
            }
            .filter { it.message == null || it.message.id == id }
            .onCompletion { activeSubscriptions.update { it - id } }
    }

    @PublishedApi
    internal suspend fun sendSerialized(message: String, connectionId: Long? = null) {
        currentCoroutineContext().ensureActive()
        val connection = checkNotNull(activeSession.value) { "WebSocket is not connected" }
        check(connectionId == null || connectionId == connection.id) { "WebSocket session changed" }
        val session = connection.session
        check(
            session.isActive &&
                    !session.closeReason.isCompleted &&
                    connectivityObserver.isOnline.value,
        ) {
            "WebSocket is not connected"
        }
        session.send(Frame.Text(message))
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
                delay(delayMs.milliseconds)
            }
        }
    }

    private suspend fun runWebSocketSession() {
        httpClient.clearAuthTokens()
        httpClient.webSocket(
            host = socketConfig.host,
            port = socketConfig.port,
            path = socketConfig.path,
            request = {
                url.protocol = if (socketConfig.secure) URLProtocol.WSS else URLProtocol.WS
            },
            block = {
                val session = this
                val connection = Connection(++nextConnectionId, session)
                activeSession.value = connection
                val sent: MutableSet<String> = mutableSetOf()
                val authorization = SocketAuthorizationClient(
                    tokenManager = tokenManager,
                    json = json,
                    timeSource = timeSource,
                    send = { update -> send(update, "auth.update", connection.id) },
                    close = {
                        close(
                            CloseReason(
                                CloseReason.Codes.VIOLATED_POLICY,
                                "Authorization refresh failed",
                            ),
                        )
                    },
                )
                val authorizationJob = launch { authorization.run() }

                val syncJob = activeSubscriptions
                    .onEach { current ->
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

                        for (newId in current.keys - sent) {
                            val message = current[newId] ?: continue
                            Log.d { "subscribe message = $message" }
                            send(Frame.Text(message))
                            sent.add(newId)
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
                        activeSession.compareAndSet(connection, null)
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
                        if (newMessage.id == SOCKET_AUTH_ID) {
                            authorization.receive(newMessage.payload)
                            continue
                        }
                        Log.d { "socket message for subscription ${newMessage.id}" }
                        incomingFlow.emit(ReceivedMessage(connection.id, newMessage))
                    }
                } finally {
                    activeSession.compareAndSet(connection, null)
                    syncJob.cancel()
                    closeGuardJob.cancel()
                    authorizationJob.cancel()
                    withContext(NonCancellable) {
                        incomingFlow.emit(ReceivedMessage(connection.id, null))
                    }
                }
            },
        )
    }

    private fun reconnectBackoffMs(attempt: Int): Long {
        val bounded = attempt.coerceIn(0, BACKOFF_MAX_SHIFTS)
        return (BACKOFF_BASE_MS shl bounded).coerceAtMost(BACKOFF_MAX_MS)
    }

    private data class Connection(val id: Long, val session: DefaultClientWebSocketSession)

    @PublishedApi
    internal data class ReceivedMessage(
        @PublishedApi internal val connectionId: Long,
        @PublishedApi internal val message: SocketMessage?,
    )
}

@PublishedApi
internal const val UNSUBSCRIBE_MESSAGE_TYPE: String = "unsubscribe"

private const val BACKOFF_BASE_MS: Long = 500L
private const val BACKOFF_MAX_MS: Long = 30_000L
private const val BACKOFF_MAX_SHIFTS: Int = 6
