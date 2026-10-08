package com.security.chat.multiplatform.common.core.network

import com.security.chat.multiplatform.common.core.network.entity.SocketConfig
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

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

    private suspend fun SocketSession.receiveMessage() =
        Json.parseToJsonElement(assertIs<Frame.Text>(outgoing.receive()).readText()).jsonObject

    private fun runManagerTest(block: suspend TestScope.(Fixture) -> Unit) =
        runTest {
            val fixture = Fixture(backgroundScope, StandardTestDispatcher(testScheduler))
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
    private class Fixture(scope: CoroutineScope, testDispatcher: CoroutineDispatcher) {
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
}
