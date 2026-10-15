package com.security.chat.multiplatform.common.core.network

import com.security.chat.multiplatform.common.core.network.entity.AccessToken
import com.security.chat.multiplatform.common.core.network.entity.RefreshToken
import com.security.chat.multiplatform.common.core.network.entity.SOCKET_AUTH_ID
import com.security.chat.multiplatform.common.core.network.entity.SocketAuthEnvelope
import com.security.chat.multiplatform.common.core.network.entity.SocketAuthState
import com.security.chat.multiplatform.common.core.network.entity.SocketAuthUpdate
import com.security.chat.multiplatform.common.core.network.entity.SocketConfig
import com.security.chat.multiplatform.common.core.network.entity.Tokens
import io.ktor.client.HttpClient
import io.ktor.client.engine.callContext
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Headers
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.InternalAPI
import io.ktor.websocket.CloseReason
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketExtension
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.TimeSource

class LiveEventsManagerTest {

    @Test
    fun `sending without a connection fails without opening a socket`() =
        runManagerTest { fixture ->
            assertFailsWith<IllegalStateException> {
                fixture.manager.send(message = mapOf("chatId" to "chat-1"), type = "call.join")
            }
            runCurrent()
            assertTrue(fixture.engine.requestHistory.isEmpty())
        }

    @Test
    fun `sending preserves nested JSON and existing subscription delivery`() =
        runManagerTest { fixture ->
            fixture.subscribe()
            runCurrent()
            val session = fixture.sessions.receive()
            val subscription = session.receiveMessage()
            val payload = mapOf("text" to "Привет, \"Борис\"!\n\\path")

            fixture.manager.send(message = payload, type = "call.signal")

            val message = session.receiveMessage()
            assertEquals(setOf("type", "payload"), message.keys)
            assertEquals("call.signal", message.getValue("type").jsonPrimitive.content)
            assertEquals(
                payload,
                Json.decodeFromString<Map<String, String>>(message.getValue("payload").jsonPrimitive.content),
            )

            session.incoming.send(
                Frame.Text(
                    Json.encodeToString(
                        mapOf(
                            "id" to subscription.getValue("id").jsonPrimitive.content,
                            "payload" to Json.encodeToString(mapOf("userId" to "boris")),
                        ),
                    ),
                ),
            )
            runCurrent()
            assertEquals(mapOf("userId" to "boris"), fixture.events.receive())
        }

    @Test
    fun `sending after the last subscription closes fails`() =
        runManagerTest { fixture ->
            val subscription = fixture.subscribe()
            runCurrent()
            fixture.sessions.receive()

            subscription.cancel()
            runCurrent()

            assertFailsWith<IllegalStateException> {
                fixture.manager.send(message = emptyMap<String, String>(), type = "call.leave")
            }
        }

    @Test
    fun `reconnection uses a new session without replaying sent messages`() =
        runManagerTest { fixture ->
            fixture.subscribe()
            runCurrent()
            val firstSession = fixture.sessions.receive()
            firstSession.outgoing.receive()
            fixture.manager.send(message = mapOf("value" to "first"), type = "call.signal")
            firstSession.outgoing.receive()

            fixture.isOnline.value = false
            runCurrent()
            assertFailsWith<IllegalStateException> {
                fixture.manager.send(message = mapOf("value" to "offline"), type = "call.signal")
            }

            fixture.isOnline.value = true
            runCurrent()
            val secondSession = fixture.sessions.receive()
            secondSession.outgoing.receive()
            assertTrue(secondSession.outgoing.tryReceive().isFailure)

            fixture.manager.send(message = mapOf("value" to "second"), type = "call.signal")
            val message = secondSession.receiveMessage()
            assertEquals(
                mapOf("value" to "second"),
                Json.decodeFromString<Map<String, String>>(message.getValue("payload").jsonPrimitive.content),
            )
        }

    @Test
    fun `commands bound to an old connection cannot reach a replacement socket`() =
        runManagerTest { fixture ->
            fixture.subscribe()
            runCurrent()
            val first = fixture.sessions.receive()
            first.outgoing.receive()
            val firstId = fixture.manager.getConnectionIdFlow().first { it != null }
            fixture.isOnline.value = false
            runCurrent()
            fixture.isOnline.value = true
            runCurrent()
            val second = fixture.sessions.receive()
            second.outgoing.receive()

            assertFailsWith<IllegalStateException> {
                fixture.manager.send(emptyMap<String, String>(), "call.join", firstId)
            }
            assertTrue(second.outgoing.tryReceive().isFailure)
        }

    private suspend fun SocketSession.receiveMessage() =
        Json.parseToJsonElement(assertIs<Frame.Text>(outgoing.receive()).readText()).jsonObject

    @Test
    fun `authorization updates preserve connection id and subscription delivery`() =
        runManagerTest { fixture ->
            fixture.subscribe()
            runCurrent()
            val socket = fixture.sessions.receive()
            val subscription = socket.receiveMessage()
            val connectionId = fixture.manager.getConnectionIdFlow().first()
            socket.authState(120_000L)
            runCurrent()
            advanceTimeBy(60_000L)
            runCurrent()
            val command = socket.receiveMessage()
            assertEquals("auth.update", command.getValue("type").jsonPrimitive.content)
            val update =
                Json.decodeFromString<SocketAuthUpdate>(command.getValue("payload").jsonPrimitive.content)
            assertEquals("refreshed-access", update.accessToken)
            socket.authState(120_000L, update.requestId)
            runCurrent()
            socket.incoming.send(
                Frame.Text(
                    Json.encodeToString(
                        mapOf(
                            "id" to subscription.getValue("id").jsonPrimitive.content,
                            "payload" to Json.encodeToString(mapOf("userId" to "peer")),
                        ),
                    ),
                ),
            )
            runCurrent()
            assertEquals(mapOf("userId" to "peer"), fixture.events.receive())
            assertEquals(connectionId, fixture.manager.getConnectionIdFlow().first())
            assertFalse(socket.closeReason.isCompleted)
            assertEquals(1, fixture.engine.requestHistory.size)
            assertTrue(socket.outgoing.tryReceive().isFailure)
        }

    private suspend fun SocketSession.authState(expiresInMillis: Long, requestId: String? = null) {
        val envelope = SocketAuthEnvelope(
            if (requestId == null) "auth.state" else "auth.updated",
            Json.encodeToString(SocketAuthState(1L, expiresInMillis, requestId)),
        )
        incoming.send(
            Frame.Text(
                Json.encodeToString(
                    mapOf(
                        "id" to SOCKET_AUTH_ID, "payload" to Json.encodeToString(envelope),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `room replacement unsubscribes before subscribing on the same socket`() =
        runManagerTest { fixture ->
            fixture.subscribe()
            val old = fixture.manager.subscribe<Map<String, String>, Map<String, String>>(
                subscribeMessage = mapOf("chatId" to "chat-1"),
                type = "call_events",
            ).launchIn(backgroundScope)
            runCurrent()
            val socket = fixture.sessions.receive()
            val original = List(2) { socket.receiveMessage() }
                .single { it.getValue("type").jsonPrimitive.content == "call_events" }
            val oldId = original.getValue("id").jsonPrimitive.content
            val events = Channel<Map<String, String>>(Channel.UNLIMITED)

            old.cancel()
            fixture.manager.subscribe<Map<String, String>, Map<String, String>>(
                subscribeMessage = mapOf("chatId" to "chat-2"),
                type = "call_events",
            ).onEach { events.send(it) }.launchIn(backgroundScope)
            runCurrent()

            val unsubscribe = socket.receiveMessage()
            assertEquals("unsubscribe", unsubscribe.getValue("type").jsonPrimitive.content)
            assertEquals(oldId, unsubscribe.getValue("payload").jsonPrimitive.content)
            val replacement = socket.receiveMessage()
            assertEquals("call_events", replacement.getValue("type").jsonPrimitive.content)
            assertEquals(
                mapOf("chatId" to "chat-2"),
                Json.decodeFromString<Map<String, String>>(
                    replacement.getValue("payload").jsonPrimitive.content,
                ),
            )
            socket.incoming.send(
                Frame.Text(
                    Json.encodeToString(
                        mapOf(
                            "id" to replacement.getValue("id").jsonPrimitive.content,
                            "payload" to Json.encodeToString(mapOf("chatId" to "chat-2")),
                        ),
                    ),
                ),
            )
            runCurrent()
            assertEquals(mapOf("chatId" to "chat-2"), events.receive())
            assertEquals(1, fixture.engine.requestHistory.size)
            assertTrue(fixture.sessions.tryReceive().isFailure)
        }

    private fun runManagerTest(block: suspend TestScope.(Fixture) -> Unit) =
        runTest {
            val fixture = Fixture(
                backgroundScope,
                StandardTestDispatcher(testScheduler),
                testScheduler.timeSource,
            )
            try {
                block(fixture)
            } finally {
                backgroundScope.cancel()
                fixture.client.close()
                fixture.engine.coroutineContext.cancel()
                fixture.engine.close()
            }
        }

    @OptIn(InternalAPI::class)
    private class Fixture(
        scope: CoroutineScope,
        testDispatcher: CoroutineDispatcher,
        timeSource: TimeSource,
    ) {
        val sessions = Channel<SocketSession>(Channel.UNLIMITED)
        val engine = MockEngine(
            MockEngineConfig().apply {
                dispatcher = testDispatcher
                addHandler {
                    val context = callContext()
                    val session = SocketSession(context)
                    sessions.send(session)
                    HttpResponseData(
                        statusCode = HttpStatusCode.SwitchingProtocols,
                        requestTime = GMTDate(),
                        headers = Headers.Empty,
                        version = HttpProtocolVersion.HTTP_1_1,
                        body = session,
                        callContext = context,
                    )
                }
            },
        )
        val client = HttpClient(engine) { install(WebSockets) }
        val isOnline = MutableStateFlow(true)
        val connectivity = object : ConnectivityObserver {
            override val isOnline = this@Fixture.isOnline
        }
        val events = Channel<Map<String, String>>(Channel.UNLIMITED)
        val tokenManager = FakeTokenManager()
        val manager = LiveEventsManager(
            json = Json,
            coroutineScope = scope,
            socketConfig = SocketConfig(
                host = "localhost",
                port = 80,
                path = "/ws",
                secure = false,
            ),
            connectivityObserver = connectivity,
            httpClientFactory = object : HttpClientFactory {
                override fun build(needAuthorization: Boolean): HttpClient = client
            },
            tokenManager = tokenManager,
            timeSource = timeSource,
        )
        private val subscriptionScope = scope

        fun subscribe() =
            manager.subscribe<Map<String, String>, Map<String, String>>(
                subscribeMessage = mapOf("chatId" to "chat-1"),
                type = "focused",
            ).onEach { events.send(it) }.launchIn(subscriptionScope)
    }

    private class SocketSession(override val coroutineContext: CoroutineContext) :
        DefaultWebSocketSession {
        override var masking = false
        override var maxFrameSize = Long.MAX_VALUE
        override var pingIntervalMillis = 0L
        override var timeoutMillis = 0L
        override val incoming = Channel<Frame>(Channel.UNLIMITED)
        override val outgoing = Channel<Frame>(Channel.UNLIMITED)
        override val extensions = emptyList<WebSocketExtension<*>>()
        override val closeReason = CompletableDeferred<CloseReason?>()

        @OptIn(InternalAPI::class)
        override fun start(negotiatedExtensions: List<WebSocketExtension<*>>) = Unit

        override suspend fun send(frame: Frame) {
            if (frame is Frame.Close) {
                closeReason.complete(CloseReason(CloseReason.Codes.NORMAL, "closed"))
                incoming.close()
                outgoing.close()
            } else {
                outgoing.send(frame)
            }
        }

        override suspend fun flush() = Unit

        @Deprecated("Use cancel() instead.")
        override fun terminate() {
            coroutineContext.cancel()
        }
    }

    private class FakeTokenManager : TokenManager {
        var tokens: Tokens? = Tokens(AccessToken("refreshed-access"), RefreshToken("refresh"))
        var refreshCalls = 0
        var refreshError: Throwable? = null
        override suspend fun saveTokens(tokens: Tokens) {
            this.tokens = tokens
        }

        override suspend fun getTokens(): Tokens? = tokens
        override suspend fun refreshTokens(): Tokens? {
            refreshCalls++
            refreshError?.let { throw it }
            return tokens
        }

        override suspend fun clearTokens() {
            tokens = null
        }
    }
}
