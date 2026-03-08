package com.security.chat.multiplatform.common.core.network

import com.security.chat.multiplatform.common.core.network.entity.SocketConfig
import com.security.chat.multiplatform.common.core.network.entity.SocketMessage
import com.security.chat.multiplatform.common.core.network.entity.SocketSubscribeMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

public class LiveEventsManager(
    @PublishedApi
    internal val json: Json,
    @PublishedApi
    internal val socketConfig: SocketConfig,
    @PublishedApi
    internal val coroutineScope: CoroutineScope,
    httpClientFactory: HttpClientFactory,
) {

    private val httpClient: HttpClient = httpClientFactory.build()
    private val socketMutex = Mutex()

    @PublishedApi
    @Volatile
    internal var isSocketOpened: Boolean = false

    @PublishedApi
    internal val incomingFlow: MutableSharedFlow<SocketMessage> = MutableSharedFlow()

    @PublishedApi
    internal val messagesToSendFlow: Channel<String> = Channel(Channel.UNLIMITED)

    @PublishedApi
    internal var socketReady: CompletableDeferred<Unit> = CompletableDeferred()

    @PublishedApi
    internal val subscribersCounter: MutableStateFlow<Int> = MutableStateFlow(0)

    @OptIn(ExperimentalUuidApi::class)
    public inline fun <reified Event, reified SubscribeMessage> subscribe(
        subscribeMessage: SubscribeMessage,
        type: String,
    ): Flow<Event> {
        val id = Uuid.random().toString()

        coroutineScope.launch {
            //just to wait
            if (!isSocketOpened) {
                launch {
                    openScopeIfClosed()
                }
                socketReady.await()
            }
            val socketSubscribeMessage = SocketSubscribeMessage(
                id = id,
                type = type,
                payload = json.encodeToString(subscribeMessage),
            )
            val valueToSend = json.encodeToString(socketSubscribeMessage)
            messagesToSendFlow.send(valueToSend)
        }

        subscribersCounter.update { it + 1 }

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
    internal suspend fun openScopeIfClosed() {
        socketMutex.withLock {
            if (isSocketOpened) return
            isSocketOpened = true
        }

        var subscribersJob: Job? = null

        try {
            httpClient.webSocket(
                host = socketConfig.host,
                port = socketConfig.port,
                path = socketConfig.path,
                block = {
                    val subscribeJob = launch {
                        for (message in messagesToSendFlow) {
                            println("qweqw message = $message")
                            send(Frame.Text(message))
                        }
                    }

                    socketReady.complete(Unit)

                    subscribersJob = subscribersCounter
                        .onEach { subscribersCount ->
                            println("qweqw subscribersCount = $subscribersCount")
                            if (subscribersCount == 0) {
                                println("qweqw close started")
                                subscribeJob.cancelAndJoin()
                                close()
                                isSocketOpened = false
                                println("qweqw close ended")
                            }
                        }
                        .launchIn(this)

                    for (receivedFrame in incoming) {
                        val othersMessage =
                            (receivedFrame as? Frame.Text) ?: error("Wrong message type: ")
                        val newMessage: SocketMessage =
                            json.decodeFromString(othersMessage.readText())
                        incomingFlow.emit(newMessage)
                    }
                },
            )
        } catch (e: Exception) {
            println("Exception in socket")
            println(e)
            isSocketOpened = false
            socketReady = CompletableDeferred()
            subscribersJob?.cancel()
        }
    }

}

@PublishedApi
internal const val UNSUBSCRIBE_MESSAGE_TYPE: String = "unsubscribe"