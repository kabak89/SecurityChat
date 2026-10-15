package com.security.chat.multiplatform.features.call.data

import com.security.chat.multiplatform.common.core.network.ConnectivityObserver
import com.security.chat.multiplatform.common.core.network.HttpClientFactory
import com.security.chat.multiplatform.common.core.network.LiveEventsManager
import com.security.chat.multiplatform.common.core.network.TokenManager
import com.security.chat.multiplatform.common.core.network.entity.SocketConfig
import com.security.chat.multiplatform.common.core.network.entity.Tokens
import com.security.chat.multiplatform.common.encryption.entity.CryptoKeys
import com.security.chat.multiplatform.features.call.data.entity.CallEnvelope
import com.security.chat.multiplatform.features.call.domain.entity.CallPeerConnection
import com.security.chat.multiplatform.features.call.domain.entity.CallPresence
import com.security.chat.multiplatform.features.call.domain.entity.CallRoomEvent
import com.security.chat.multiplatform.features.call.domain.entity.CallSignal
import com.security.chat.multiplatform.features.call.domain.entity.errors.CallSignalingError
import com.security.chat.multiplatform.features.call.domain.repo.CallSession
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import com.security.chat.multiplatform.features.users.data.common.UsersDataHelper
import com.security.chat.multiplatform.features.users.data.common.entity.UserInfo
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
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CallSessionTest {
    @Test
    fun `focused presence and call events share one socket connection`() = runSessionTest { f ->
        f.manager.subscribe<Map<String, String>, Map<String, String>>(
            subscribeMessage = mapOf("chatId" to "chat"),
            type = "focused",
        ).launchIn(backgroundScope)
        runCurrent()
        val socket = f.sessions.receive()
        assertEquals("focused", socket.receiveMessage().getValue("type").jsonPrimitive.content)

        f.callSession.events.launchIn(backgroundScope)
        runCurrent()
        assertEquals("call_events", socket.receiveMessage().getValue("type").jsonPrimitive.content)
        assertEquals(1, f.engine.requestHistory.size)
        assertTrue(f.sessions.tryReceive().isFailure)
    }

    @Test
    fun `subscription join and media signals use the server wire format`() = runSessionTest { f ->
        val events = Channel<CallRoomEvent>(Channel.UNLIMITED)
        f.callSession.events.onEach { events.send(it) }.launchIn(backgroundScope)
        runCurrent()
        val socket = f.sessions.receive()
        val subscription = socket.receiveMessage()
        assertEquals("call_events", subscription.getValue("type").jsonPrimitive.content)
        assertEquals("/ws", f.engine.requestHistory.single().url.encodedPath)
        val id = subscription.getValue("id").jsonPrimitive.content
        socket.event(
            id,
            "call.state",
            """{"chatId":"chat","participants":[],"connections":[],"iceServers":[]}""",
        )
        runCurrent()
        assertIs<CallRoomEvent.Snapshot>(events.receive())
        f.callSession.join("request")
        val join = socket.receiveMessage()
        assertEquals(setOf("type", "payload"), join.keys)
        assertEquals("call.join", join.getValue("type").jsonPrimitive.content)
        val joinPayload =
            Json.parseToJsonElement(join.getValue("payload").jsonPrimitive.content).jsonObject
        assertEquals("chat", joinPayload.getValue("chatId").jsonPrimitive.content)
        assertEquals("request", joinPayload.getValue("requestId").jsonPrimitive.content)
        socket.event(
            id,
            "call.joined",
            """{"chatId":"chat","callId":"call","participantSessionId":"me","requestId":"request","connections":[{"id":"edge","participantSessionId":"peer","isInitiator":true}],"iceServers":[{"urls":["turn:example.test"],"username":"u","credential":"p"}]}""",
        )
        runCurrent()
        val joined = assertIs<CallRoomEvent.Joined>(events.receive())
        assertEquals("p", joined.snapshot.iceServers.single().credential)
        val peer = joined.snapshot.connections.single().connection
        f.callSession.sendSignal("call", CallSignal.Offer(peer, "v=0\r\n"))
        val offer = socket.receiveMessage()
        assertEquals("call.signal", offer.getValue("type").jsonPrimitive.content)
        val payload =
            Json.parseToJsonElement(offer.getValue("payload").jsonPrimitive.content).jsonObject
        assertEquals("peer", payload.getValue("participantSessionId").jsonPrimitive.content)
        assertEquals("edge", payload.getValue("connectionId").jsonPrimitive.content)
        assertEquals("v=0\r\n", payload.getValue("sdp").jsonPrimitive.content)
        f.callSession.leave("call")
        assertEquals("call.leave", socket.receiveMessage().getValue("type").jsonPrimitive.content)
    }

    @Test
    fun `signal bursts preserve order without conflation and ignore acknowledgements`() =
        runSessionTest { f ->
            val events = Channel<CallRoomEvent>(Channel.UNLIMITED)
            f.callSession.events.onEach { events.send(it) }.launchIn(backgroundScope)
            runCurrent()
            val socket = f.sessions.receive()
            val id = socket.receiveMessage().getValue("id").jsonPrimitive.content
            repeat(80) { index ->
                socket.event(
                    id,
                    "call.signal",
                    """{"chatId":"chat","callId":"call","connectionId":"edge","participantSessionId":"peer","signalType":"ice_candidate","candidate":"candidate-$index","sdpMid":"0","sdpMLineIndex":0}""",
                )
            }
            socket.event(id, "call.ack", """{"requestId":"ack","command":"call.signal"}""")
            runCurrent()
            repeat(80) { index ->
                val event = assertIs<CallRoomEvent.Signal>(events.receive())
                assertEquals(
                    "candidate-$index",
                    assertIs<CallSignal.IceCandidate>(event.signal).candidate,
                )
            }
            assertTrue(events.tryReceive().isFailure)
        }

    @Test
    fun `socket loss fails the active session`() = runSessionTest { f ->
        val errors = Channel<Throwable>(Channel.UNLIMITED)
        f.callSession.events.catch { errors.send(it) }.launchIn(backgroundScope)
        runCurrent()
        val socket = f.sessions.receive()
        socket.receiveMessage()
        f.isOnline.value = false
        runCurrent()
        assertEquals("DISCONNECTED", assertIs<CallSignalingError>(errors.receive()).code)
    }

    @Test
    fun `signal errors retain their peer so a departed connection can be ignored`() =
        runSessionTest { f ->
            val events = Channel<CallRoomEvent>(Channel.UNLIMITED)
            f.callSession.events.onEach { events.send(it) }.launchIn(backgroundScope)
            runCurrent()
            val socket = f.sessions.receive()
            val id = socket.receiveMessage().getValue("id").jsonPrimitive.content
            socket.event(id, "call.state", """{"chatId":"chat"}""")
            runCurrent()
            events.receive()
            val peer = CallPeerConnection(
                "edge", "peer",
            )
            f.callSession.sendSignal("call", CallSignal.IceGatheringComplete(peer))
            val payload = Json.parseToJsonElement(
                socket.receiveMessage().getValue("payload").jsonPrimitive.content,
            ).jsonObject
            val requestId = payload.getValue("requestId").jsonPrimitive.content
            socket.event(
                id, "call.error",
                protocolJson.encodeToString(
                    mapOf(
                        "requestId" to requestId,
                        "code" to "UNKNOWN_PARTICIPANT",
                        "message" to "Participant left",
                    ),
                ),
            )
            runCurrent()
            val error = assertIs<CallRoomEvent.SignalRejected>(events.receive())
            assertEquals(peer, error.connection)
            assertEquals("UNKNOWN_PARTICIPANT", error.code)
        }

    @Test
    fun `server errors are surfaced instead of waiting forever for join`() = runSessionTest { f ->
        val events = Channel<CallRoomEvent>(Channel.UNLIMITED)
        f.callSession.events.onEach { events.send(it) }.launchIn(backgroundScope)
        runCurrent()
        val socket = f.sessions.receive()
        val id = socket.receiveMessage().getValue("id").jsonPrimitive.content
        socket.event(
            id,
            "call.error",
            """{"code":"NOT_ELIGIBLE","message":"Focus required","requestId":"join"}""",
        )
        runCurrent()
        assertEquals("NOT_ELIGIBLE", assertIs<CallRoomEvent.Error>(events.receive()).code)
    }

    @Test
    fun `watching the screen shows call membership without joining audio`() = runSessionTest { f ->
        val presence = MutableStateFlow<CallPresence?>(null)
        f.repo.getPresenceFlow("chat").onEach { presence.value = it }.launchIn(backgroundScope)
        runCurrent()
        val socket = f.sessions.receive()
        val ids = socket.subscriptionIds()
        socket.focused(ids.getValue("focused"))
        socket.event(
            ids.getValue("call_events"), "call.state",
            """{"chatId":"chat","callId":"call","participants":[{"participantSessionId":"peer-session","userId":"peer"}]}""",
        )
        runCurrent()
        val current = requireNotNull(presence.value)
        assertTrue(current.isCallInProgress)
        assertEquals(
            mapOf("peer" to true, "other" to false),
            current.participants.associate { it.id to it.isInCall },
        )
        assertEquals(1, f.engine.requestHistory.size)
        assertTrue(socket.outgoing.tryReceive().isFailure)

        socket.event(
            ids.getValue("call_events"), "call.state",
            """{"chatId":"chat","participants":[]}""",
        )
        runCurrent()
        assertFalse(requireNotNull(presence.value).isCallInProgress)
        assertTrue(requireNotNull(presence.value).participants.none { it.isInCall })
    }

    @Test
    fun `participant remains in call until their last device leaves`() = runSessionTest { f ->
        val presence = MutableStateFlow<CallPresence?>(null)
        f.repo.getPresenceFlow("chat").onEach { presence.value = it }.launchIn(backgroundScope)
        runCurrent()
        val socket = f.sessions.receive()
        val ids = socket.subscriptionIds()
        socket.focused(ids.getValue("focused"))
        socket.event(
            ids.getValue("call_events"), "call.state",
            """{"chatId":"chat","callId":"call","participants":[{"participantSessionId":"p1","userId":"peer"},{"participantSessionId":"p2","userId":"peer"}]}""",
        )
        runCurrent()
        assertTrue(requireNotNull(presence.value).participants.first { it.id == "peer" }.isInCall)
        socket.event(
            ids.getValue("call_events"), "call.state",
            """{"chatId":"chat","callId":"call","participants":[{"participantSessionId":"p2","userId":"peer"}]}""",
        )
        runCurrent()
        assertTrue(requireNotNull(presence.value).participants.first { it.id == "peer" }.isInCall)
    }

    @Test
    fun `audio session reuses room subscription and hangup keeps watching`() = runSessionTest { f ->
        f.repo.getPresenceFlow("chat").launchIn(backgroundScope)
        runCurrent()
        val socket = f.sessions.receive()
        val ids = socket.subscriptionIds()
        socket.focused(ids.getValue("focused"))
        socket.event(ids.getValue("call_events"), "call.state", """{"chatId":"chat"}""")
        runCurrent()
        val session = CompletableDeferred<CallSession>()
        val events = Channel<CallRoomEvent>(Channel.UNLIMITED)
        val job = backgroundScope.launch {
            f.repo.withCallSession("chat") {
                session.complete(it)
                it.events.collect { event -> events.send(event) }
            }
        }
        runCurrent()
        assertIs<CallRoomEvent.Snapshot>(events.receive())
        session.await().join("join")
        assertEquals("call.join", socket.receiveMessage().getValue("type").jsonPrimitive.content)
        socket.event(
            ids.getValue("call_events"), "call.joined",
            """{"chatId":"chat","callId":"call","participantSessionId":"mine","requestId":"join","participants":[{"participantSessionId":"mine","userId":"me"}]}""",
        )
        runCurrent()
        assertIs<CallRoomEvent.Joined>(events.receive())
        job.cancelAndJoin()
        runCurrent()
        assertEquals("call.leave", socket.receiveMessage().getValue("type").jsonPrimitive.content)
        assertTrue(socket.outgoing.tryReceive().isFailure)
        socket.event(ids.getValue("call_events"), "call.state", """{"chatId":"chat"}""")
        runCurrent()
        f.repo.withCallSession("chat") {
            assertIs<CallRoomEvent.Snapshot>(it.events.first())
        }
        assertTrue(socket.outgoing.tryReceive().isFailure)
    }

    @Test
    fun `late join reply after hangup is left without enabling a new call`() = runSessionTest { f ->
        f.repo.getPresenceFlow("chat").launchIn(backgroundScope)
        runCurrent()
        val socket = f.sessions.receive()
        val ids = socket.subscriptionIds()
        socket.focused(ids.getValue("focused"))
        socket.event(ids.getValue("call_events"), "call.state", """{"chatId":"chat"}""")
        runCurrent()
        val job = backgroundScope.launch {
            f.repo.withCallSession("chat") {
                it.events.first()
                it.join("join")
                awaitCancellation()
            }
        }
        runCurrent()
        assertEquals("call.join", socket.receiveMessage().getValue("type").jsonPrimitive.content)
        job.cancelAndJoin()
        socket.event(
            ids.getValue("call_events"), "call.joined",
            """{"chatId":"chat","callId":"call","participantSessionId":"mine","requestId":"join"}""",
        )
        runCurrent()
        assertEquals("call.leave", socket.receiveMessage().getValue("type").jsonPrimitive.content)
    }

    private suspend fun SocketSession.subscriptionIds(): Map<String, String> =
        List(2) { receiveMessage() }.associate {
            it.getValue("type").jsonPrimitive.content to it.getValue("id").jsonPrimitive.content
        }

    @Test
    fun `rejected join fails the call but room updates still reach spectators`() =
        runSessionTest { f ->
            val presence = MutableStateFlow<CallPresence?>(null)
            f.repo.getPresenceFlow("chat").onEach { presence.value = it }.launchIn(backgroundScope)
            runCurrent()
            val socket = f.sessions.receive()
            val ids = socket.subscriptionIds()
            socket.focused(ids.getValue("focused"))
            socket.event(ids.getValue("call_events"), "call.state", """{"chatId":"chat"}""")
            runCurrent()
            val errors = Channel<Throwable>(Channel.UNLIMITED)
            val job = backgroundScope.launch {
                f.repo.withCallSession("chat") {
                    it.events.catch { error -> errors.send(error) }.collect { }
                }
            }
            runCurrent()
            socket.event(
                ids.getValue("call_events"), "call.error",
                """{"code":"NOT_ELIGIBLE","message":"Focus required","requestId":"join"}""",
            )
            runCurrent()
            job.join()
            assertEquals("NOT_ELIGIBLE", assertIs<CallSignalingError>(errors.receive()).code)
            socket.event(
                ids.getValue("call_events"), "call.state",
                """{"chatId":"chat","callId":"call","participants":[{"participantSessionId":"peer-session","userId":"peer"}]}""",
            )
            runCurrent()
            assertTrue(requireNotNull(presence.value).participants.first { it.id == "peer" }.isInCall)
            assertTrue(requireNotNull(presence.value).isCallInProgress)
            assertTrue(socket.outgoing.tryReceive().isFailure)
        }

    private suspend fun SocketSession.focused(id: String) {
        incoming.send(
            Frame.Text(
                protocolJson.encodeToString(
                    mapOf(
                        "id" to id,
                        "payload" to """{"chatId":"chat","userIds":["me","peer","other"]}""",
                    ),
                ),
            ),
        )
    }

    @Test
    fun `reconnection restores room watching without automatically joining audio`() =
        runSessionTest { f ->
            val presence = MutableStateFlow<CallPresence?>(null)
            f.repo.getPresenceFlow("chat").onEach { presence.value = it }.launchIn(backgroundScope)
            runCurrent()
            val first = f.sessions.receive()
            val firstIds = first.subscriptionIds()
            first.focused(firstIds.getValue("focused"))
            first.event(
                firstIds.getValue("call_events"), "call.state",
                """{"chatId":"chat","callId":"call","participants":[{"participantSessionId":"p1","userId":"peer"}]}""",
            )
            runCurrent()
            assertTrue(requireNotNull(presence.value).isCallInProgress)
            val errors = Channel<Throwable>(Channel.UNLIMITED)
            backgroundScope.launch {
                f.repo.withCallSession("chat") {
                    it.events.catch { error -> errors.send(error) }.collect { }
                }
            }
            runCurrent()
            f.isOnline.value = false
            runCurrent()
            assertEquals("DISCONNECTED", assertIs<CallSignalingError>(errors.receive()).code)
            assertFalse(requireNotNull(presence.value).isCallInProgress)
            f.isOnline.value = true
            advanceTimeBy(501)
            runCurrent()
            val second = f.sessions.receive()
            val secondIds = second.subscriptionIds()
            second.focused(secondIds.getValue("focused"))
            second.event(secondIds.getValue("call_events"), "call.state", """{"chatId":"chat"}""")
            runCurrent()
            assertFalse(requireNotNull(presence.value).isCallInProgress)
            assertTrue(requireNotNull(presence.value).participants.none { it.isInCall })
            assertEquals(2, f.engine.requestHistory.size)
            assertTrue(second.outgoing.tryReceive().isFailure)
        }

    private suspend fun SocketSession.event(id: String, type: String, payload: String) {
        incoming.send(
            Frame.Text(
                protocolJson.encodeToString(
                    mapOf(
                        "id" to id,
                        "payload" to protocolJson.encodeToString(CallEnvelope(type, payload)),
                    ),
                ),
            ),
        )
    }

    private suspend fun SocketSession.receiveMessage() =
        Json.parseToJsonElement(assertIs<Frame.Text>(outgoing.receive()).readText()).jsonObject

    private fun runSessionTest(block: suspend TestScope.(Fixture) -> Unit) = runTest {
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
        val manager = LiveEventsManager(
            json = protocolJson,
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
            tokenManager = object : TokenManager {
                override suspend fun saveTokens(tokens: Tokens) = Unit
                override suspend fun getTokens(): Tokens? = null
                override suspend fun refreshTokens(): Tokens? = null
                override suspend fun clearTokens() = Unit
            },
        )
        val callSession = CallSessionImpl("chat", manager, protocolJson)
        val repo = CallRepoImpl(
            manager, FakeUserStorage(),
            object : UsersDataHelper {
                override suspend fun getOrFetchUser(userId: String): UserInfo =
                    UserInfo(userId, userId)
            },
            protocolJson,
        )
    }

    private class FakeUserStorage : UserStorage {
        override suspend fun getUserId(): String = "me"
        override suspend fun isUserAuthorized(): Boolean = true
        override suspend fun saveUserId(userId: String) = Unit
        override suspend fun saveKeys(cryptoKeys: CryptoKeys) = Unit
        override suspend fun getKeys(): CryptoKeys? = null
        override suspend fun saveUserName(name: String) = Unit
        override suspend fun getUserName(): String = "me"
        override suspend fun saveOnboardingPassed() = Unit
        override suspend fun getIsOnboardingPassed(): Boolean = true
        override suspend fun saveDeviceId(id: String) = Unit
        override suspend fun getDeviceId(): String = "device"
        override suspend fun clearAll() = Unit
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

private val protocolJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
