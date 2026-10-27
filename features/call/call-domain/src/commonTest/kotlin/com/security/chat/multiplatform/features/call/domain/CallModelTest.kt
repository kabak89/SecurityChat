package com.security.chat.multiplatform.features.call.domain

import com.security.chat.multiplatform.common.core.domain.asLceState
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.call.domain.entity.CallConnectionState
import com.security.chat.multiplatform.features.call.domain.entity.CallIceServer
import com.security.chat.multiplatform.features.call.domain.entity.CallManagerState
import com.security.chat.multiplatform.features.call.domain.entity.CallParticipant
import com.security.chat.multiplatform.features.call.domain.entity.CallPeerConnection
import com.security.chat.multiplatform.features.call.domain.entity.CallPresence
import com.security.chat.multiplatform.features.call.domain.entity.CallRoomConnection
import com.security.chat.multiplatform.features.call.domain.entity.CallRoomEvent
import com.security.chat.multiplatform.features.call.domain.entity.CallRoomSnapshot
import com.security.chat.multiplatform.features.call.domain.entity.CallSignal
import com.security.chat.multiplatform.features.call.domain.entity.CallStatus
import com.security.chat.multiplatform.features.call.domain.entity.errors.CallMediaError
import com.security.chat.multiplatform.features.call.domain.entity.errors.CallSignalingError
import com.security.chat.multiplatform.features.call.domain.repo.CallRepo
import com.security.chat.multiplatform.features.call.domain.repo.CallSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CallModelTest {
    @Test
    fun `failed peer SDP keeps another connected peer and the microphone active`() = runTest {
        val f = fixture()
        f.start()
        val other = CallPeerConnection("other", "other-session")
        f.addPeer(other)
        f.manager.signalFailures += other
        f.session.eventsChannel.send(
            CallRoomEvent.Signal(
                "call",
                CallSignal.Offer(other, "bad-sdp"),
            ),
        )
        val validSignal = CallSignal.Answer(peer, "valid-answer")
        f.session.eventsChannel.send(CallRoomEvent.Signal("call", validSignal))
        runCurrent()
        assertEquals(CallConnectionState.Failed, f.manager.state.value.connections[other])
        assertEquals(CallConnectionState.Connected, f.manager.state.value.connections[peer])
        assertEquals(listOf<CallSignal>(validSignal), f.manager.received)
        assertTrue(f.manager.state.value.isMicrophoneEnabled)
        assertEquals(CallStatus.Active, f.model.getCallStatusFlow().first())
        assertTrue(f.session.left.isEmpty())
        assertEquals(0, f.repo.closeCount)
        f.model.endCall.start().join()
    }

    @Test
    fun `peer signaling rejections keep remaining audio active`() = runTest {
        for (code in listOf(
            "UNKNOWN_PARTICIPANT", "STALE_CONNECTION", "INVALID_SIGNAL",
            "INVALID_SIGNAL_STATE", "NOT_ELIGIBLE",
        )) {
            val f = fixture()
            f.start()
            val other = CallPeerConnection("other", "other-session")
            f.addPeer(other)
            f.session.eventsChannel.send(CallRoomEvent.SignalRejected(other, code))
            runCurrent()
            assertEquals(listOf(other), f.manager.failed)
            assertEquals(CallConnectionState.Failed, f.manager.state.value.connections[other])
            assertEquals(CallStatus.Active, f.model.getCallStatusFlow().first())
            assertTrue(f.session.left.isEmpty())
            assertEquals(0, f.repo.closeCount)
            f.model.endCall.start().join()
        }
    }

    @Test
    fun `loss of the server call session still ends every peer connection`() = runTest {
        for (code in listOf("STALE_CALL", "NOT_JOINED", "NOT_SUBSCRIBED")) {
            val f = fixture()
            f.start()
            f.addPeer(CallPeerConnection("other", "other-session"))
            f.session.eventsChannel.send(CallRoomEvent.SignalRejected(peer, code))
            runCurrent()
            assertEquals(CallStatus.Idle, f.model.getCallStatusFlow().first())
            assertEquals(CallManagerState(), f.manager.state.value)
            assertEquals(listOf("call"), f.session.left)
            assertEquals(1, f.repo.closeCount)
        }
    }

    @Test
    fun `call ends when all expected peer connections fail`() = runTest {
        val f = fixture()
        f.start()
        val other = CallPeerConnection("other", "other-session")
        f.addPeer(other)
        f.session.eventsChannel.send(CallRoomEvent.SignalRejected(other, "INVALID_SIGNAL"))
        f.session.eventsChannel.send(CallRoomEvent.SignalRejected(peer, "INVALID_SIGNAL"))
        runCurrent()
        assertEquals(CallStatus.Idle, f.model.getCallStatusFlow().first())
        assertEquals(CallManagerState(), f.manager.state.value)
        assertEquals(listOf("call"), f.session.left)
        assertEquals(1, f.repo.closeCount)
        assertIs<CallMediaError.Connection>(f.callErrors.single())
    }

    @Test
    fun `audio stopping reports an audio error instead of a connection error`() = runTest {
        val f = fixture()
        f.start()
        f.manager.state.value = CallManagerState()
        runCurrent()
        assertIs<CallMediaError.Audio>(f.callErrors.single())
        assertEquals(CallStatus.Idle, f.model.getCallStatusFlow().first())
        assertEquals(listOf("call"), f.session.left)
    }

    @Test
    fun `microphone permission failure is preserved after call cleanup`() = runTest {
        val f = fixture()
        val error = CallMediaError.MicrophonePermission()
        f.manager.startError = error
        f.model.startCall.start()
        runCurrent()
        assertSame(error, f.callErrors.single())
        assertEquals(CallManagerState(), f.manager.state.value)
        assertEquals(listOf("call"), f.session.left)
    }

    @Test
    fun `failed first peer keeps the call while other peers are pending`() = runTest {
        val f = fixture(autoJoin = false)
        val other = CallPeerConnection("other", "other-session")
        f.manager.connectFailures += peer
        f.manager.yieldAfterConnect = true
        f.model.startCall.start()
        runCurrent()
        f.session.eventsChannel.send(
            CallRoomEvent.Joined(
                "join",
                joined.copy(
                    connections = joined.connections + CallRoomConnection(other, false),
                ),
            ),
        )
        runCurrent()
        assertTrue(f.manager.state.value.isStarted)
        assertEquals(CallConnectionState.Failed, f.manager.state.value.connections[peer])
        assertEquals(CallConnectionState.Connecting, f.manager.state.value.connections[other])
        f.manager.state.value = f.manager.state.value.copy(
            connections =
                f.manager.state.value.connections + (other to CallConnectionState.Connected),
        )
        assertEquals(CallStatus.Active, f.model.getCallStatusFlow().first())
        assertTrue(f.session.left.isEmpty())
        f.model.endCall.start().join()
    }

    @Test
    fun `room presence updates participant statuses while local audio stays idle`() = runTest {
        val f = fixture()
        runCurrent()
        val current = readyPresence.copy(
            participants = listOf(
                CallParticipant(id = "peer", name = "Peer", isInCall = true),
                CallParticipant(id = "other", name = "Other", isInCall = false),
            ),
            isCallInProgress = true,
        )
        f.repo.presence.value = current
        runCurrent()
        assertEquals(current, f.model.getPresenceFlow().first())
        assertEquals(CallStatus.Idle, f.model.getCallStatusFlow().first())
        assertEquals(0, f.manager.startCount)
        f.repo.presence.value = readyPresence
        runCurrent()
        assertFalse(f.model.getPresenceFlow().first().isCallInProgress)
    }

    @Test
    fun `start waits for focused presence and server join before enabling audio`() = runTest {
        val f = fixture(autoJoin = false)
        f.repo.presence.value = CallPresence(
            connectionId = null,
            isFocused = false,
            participants = emptyList(),
            isCallInProgress = false,
        )
        f.model.startCall.start()
        runCurrent()
        assertEquals(CallStatus.Connecting, f.model.getCallStatusFlow().first())
        assertEquals(0, f.repo.openCount)
        f.repo.presence.value = readyPresence
        runCurrent()
        assertEquals(1, f.session.joinCount)
        assertEquals(0, f.manager.startCount)
        f.session.eventsChannel.send(CallRoomEvent.Joined("join", joined))
        runCurrent()
        assertEquals(joined.iceServers, f.manager.servers)
        assertEquals(listOf(peer to true), f.manager.connected)
        assertTrue(f.manager.signals.subscriptionCount.value > 0)
        f.model.endCall.start().join()
    }

    @Test
    fun `snapshots register peers before signals and remove departed peers`() = runTest {
        val f = fixture()
        f.start()
        val other = CallPeerConnection("other", "other-session")
        val added = joined.copy(connections = joined.connections + CallRoomConnection(other, false))
        f.session.eventsChannel.send(CallRoomEvent.Snapshot(added))
        val offer = CallSignal.Offer(other, "remote-offer")
        f.session.eventsChannel.send(CallRoomEvent.Signal("call", offer))
        runCurrent()
        assertEquals(listOf(peer to true, other to false), f.manager.connected)
        assertEquals(listOf<CallSignal>(offer), f.manager.received)
        f.session.eventsChannel.send(CallRoomEvent.Snapshot(joined))
        runCurrent()
        assertEquals(listOf(other), f.manager.disconnected)
        f.model.endCall.start().join()
    }

    @Test
    fun `late rejection for a departed peer does not end the room call`() = runTest {
        val f = fixture()
        f.start()
        f.session.eventsChannel.send(CallRoomEvent.Snapshot(joined.copy(connections = emptyList())))
        f.session.eventsChannel.send(CallRoomEvent.SignalRejected(peer, "UNKNOWN_PARTICIPANT"))
        runCurrent()
        assertTrue(f.manager.state.value.isStarted)
        assertEquals(0, f.repo.closeCount)
        f.model.endCall.start().join()
    }

    @Test
    fun `outgoing signals are sent once and obsolete signals are ignored`() = runTest {
        val f = fixture()
        f.start()
        val answer = CallSignal.Answer(peer, "answer")
        f.session.eventsChannel.send(CallRoomEvent.Signal("old-call", answer))
        f.session.eventsChannel.send(
            CallRoomEvent.Signal(
                "call",
                CallSignal.Answer(CallPeerConnection("old", "remote"), "old"),
            ),
        )
        f.manager.signals.emit(CallSignal.Offer(peer, "offer"))
        runCurrent()
        assertTrue(f.manager.received.isEmpty())
        assertEquals(
            listOf<Pair<String, CallSignal>>("call" to CallSignal.Offer(peer, "offer")),
            f.session.sent,
        )
        f.model.endCall.start().join()
    }

    @Test
    fun `peer connection state drives active status`() = runTest {
        val f = fixture()
        f.start()
        assertEquals(CallStatus.Connecting, f.model.getCallStatusFlow().first())
        f.manager.state.value = f.manager.state.value.copy(
            connections = mapOf(peer to CallConnectionState.Connected),
        )
        assertEquals(CallStatus.Active, f.model.getCallStatusFlow().first())
        f.model.endCall.start().join()
        assertEquals(CallStatus.Idle, f.model.getCallStatusFlow().first())
        assertEquals(listOf("call"), f.session.left)
        assertFalse(f.manager.state.value.isMicrophoneEnabled)
        assertEquals(1, f.repo.closeCount)
    }

    @Test
    fun `hangup before join reply closes socket without starting media`() = runTest {
        val f = fixture(autoJoin = false)
        f.model.startCall.start()
        runCurrent()
        f.model.endCall.start().join()
        f.session.eventsChannel.send(CallRoomEvent.Joined("join", joined))
        runCurrent()
        assertEquals(0, f.manager.startCount)
        assertEquals(1, f.repo.closeCount)
        assertEquals(CallStatus.Idle, f.model.getCallStatusFlow().first())
    }

    @Test
    fun `failed media start leaves room and cleans up partial audio`() = runTest {
        val f = fixture()
        val error = CallMediaError.Audio("Media start failed")
        f.manager.startError = error
        val job = f.model.startCall.start()
        runCurrent()
        job.join()
        assertEquals(listOf("call"), f.session.left)
        assertEquals(CallManagerState(), f.manager.state.value)
        assertEquals(1, f.repo.closeCount)
        assertSame(error, f.callErrors.single())
    }

    @Test
    fun `leaving screen cancels a suspended media start`() = runTest {
        val f = fixture()
        f.manager.suspendStart = true
        f.model.startCall.start()
        runCurrent()
        assertTrue(f.manager.state.value.isStarted)
        f.model.setFocused(false)
        runCurrent()
        assertEquals(CallManagerState(), f.manager.state.value)
        assertEquals(listOf("call"), f.session.left)
        assertEquals(1, f.repo.closeCount)
    }

    @Test
    fun `transport failure stops audio and never automatically rejoins`() = runTest {
        val f = fixture()
        f.start()
        f.session.eventsChannel.close(CallSignalingError("DISCONNECTED"))
        runCurrent()
        assertEquals(CallManagerState(), f.manager.state.value)
        assertEquals(1, f.session.joinCount)
        assertEquals(1, f.repo.closeCount)
        assertEquals(CallStatus.Idle, f.model.getCallStatusFlow().first())
    }

    @Test
    fun `presence transport replacement ends the call even if focus stays true`() = runTest {
        val f = fixture()
        f.start()
        f.repo.presence.value = readyPresence.copy(connectionId = 2)
        runCurrent()
        assertEquals(CallManagerState(), f.manager.state.value)
        assertEquals(1, f.repo.closeCount)
    }

    @Test
    fun `server revocation and model destruction release media`() = runTest {
        val f = fixture()
        f.start()
        f.session.eventsChannel.send(CallRoomEvent.Revoked("membership_lost"))
        runCurrent()
        assertEquals(CallManagerState(), f.manager.state.value)
        val next = fixture()
        next.start()
        next.lifetime.cancelAndJoin()
        assertEquals(CallManagerState(), next.manager.state.value)
        assertEquals(1, next.repo.closeCount)
    }

    @Test
    fun `joining times out and closes signaling instead of leaving a pending call`() = runTest {
        val f = fixture(autoJoin = false)
        f.model.startCall.start()
        runCurrent()
        advanceTimeBy(15_001)
        runCurrent()
        assertEquals(CallStatus.Idle, f.model.getCallStatusFlow().first())
        assertEquals(0, f.manager.startCount)
        assertEquals(1, f.repo.closeCount)
    }

    private fun TestScope.fixture(autoJoin: Boolean = true): Fixture =
        Fixture(this, StandardTestDispatcher(testScheduler), autoJoin)

    private class Fixture(
        scope: TestScope,
        dispatcher: CoroutineDispatcher,
        autoJoin: Boolean,
    ) {
        val session = FakeSession(autoJoin)
        val repo = FakeRepo(session)
        val manager = FakeCallManager()
        val model: CallModel = CallModelImpl(
            repo, manager,
            object : DispatcherProviderInterface {
                override val Default = dispatcher
                override val IO = dispatcher
                override val Unconfined = dispatcher
                override val Main = Dispatchers.Main
            },
        )
        val lifetime = model.start(scope.backgroundScope)
        val callErrors = mutableListOf<Throwable>()
        private val errorObservation = model.startCall.jobFlow.asLceState()
            .onEach { result -> result.error?.let { callErrors += it } }
            .launchIn(scope.backgroundScope)
        private val testScope = scope

        init {
            model.setChatId("chat")
            model.setFocused(true)
        }

        fun start() {
            model.startCall.start()
            testScope.runCurrent()
            assertTrue(manager.state.value.isStarted)
        }

        suspend fun addPeer(other: CallPeerConnection) {
            session.eventsChannel.send(
                CallRoomEvent.Snapshot(
                    joined.copy(
                        connections = joined.connections + CallRoomConnection(other, false),
                    ),
                ),
            )
            testScope.runCurrent()
            manager.state.value = manager.state.value.copy(
                connections =
                    manager.state.value.connections + (peer to CallConnectionState.Connected),
            )
            testScope.runCurrent()
        }
    }

    private class FakeRepo(val session: FakeSession) : CallRepo {
        val presence = MutableStateFlow(readyPresence)
        var openCount = 0
        var closeCount = 0
        override fun getPresenceFlow(chatId: String): Flow<CallPresence> = presence
        override suspend fun withCallSession(chatId: String, block: suspend (CallSession) -> Unit) {
            openCount++
            try {
                session.eventsChannel.send(
                    CallRoomEvent.Snapshot(
                        CallRoomSnapshot(null, null, emptyList(), emptyList(), emptySet()),
                    ),
                )
                block(session)
            } finally {
                closeCount++
            }
        }
    }

    private class FakeSession(val autoJoin: Boolean) : CallSession {
        val eventsChannel = Channel<CallRoomEvent>(Channel.UNLIMITED)
        override val events = eventsChannel.receiveAsFlow()
        val left = mutableListOf<String>()
        val sent = mutableListOf<Pair<String, CallSignal>>()
        var joinCount = 0
        override suspend fun join(requestId: String) {
            joinCount++
            if (autoJoin) eventsChannel.send(CallRoomEvent.Joined(requestId, joined))
        }

        override suspend fun leave(callId: String) {
            left += callId
        }

        override suspend fun sendSignal(callId: String, signal: CallSignal) {
            sent += callId to signal
        }
    }

    private class FakeCallManager : CallManager {
        val state = MutableStateFlow(CallManagerState())
        val signals = MutableSharedFlow<CallSignal>()
        var servers = emptyList<CallIceServer>()
        val connected = mutableListOf<Pair<CallPeerConnection, Boolean>>()
        val received = mutableListOf<CallSignal>()
        val disconnected = mutableListOf<CallPeerConnection>()
        val failed = mutableListOf<CallPeerConnection>()
        val connectFailures = mutableSetOf<CallPeerConnection>()
        val signalFailures = mutableSetOf<CallPeerConnection>()
        var yieldAfterConnect = false
        var startCount = 0
        var startError: Throwable? = null
        var suspendStart = false
        override fun getStateFlow() = state
        override fun getOutgoingSignalsFlow() = signals
        override suspend fun start(iceServers: List<CallIceServer>) {
            startCount++
            servers = iceServers
            state.value = CallManagerState(isStarted = true, isMicrophoneEnabled = true)
            if (suspendStart) awaitCancellation()
            startError?.let { throw it }
        }

        override suspend fun connect(connection: CallPeerConnection, isInitiator: Boolean) {
            connected += connection to isInitiator
            state.value = state.value.copy(
                connections = state.value.connections +
                        (connection to CallConnectionState.Connecting),
            )
            if (connection in connectFailures) {
                failConnection(connection, CallMediaError.Connection("Peer creation failed"))
            }
            if (yieldAfterConnect) yield()
        }

        override suspend fun handleSignal(signal: CallSignal) {
            check(connected.any { it.first == signal.connection })
            if (state.value.connections[signal.connection] == CallConnectionState.Failed) return
            if (signal.connection in signalFailures) {
                failConnection(
                    signal.connection,
                    CallMediaError.Connection("SDP application failed"),
                )
                return
            }
            received += signal
        }

        override suspend fun failConnection(connection: CallPeerConnection, error: Throwable) {
            failed += connection
            state.value = state.value.copy(
                connections = state.value.connections +
                        (connection to CallConnectionState.Failed),
            )
        }

        override suspend fun disconnect(connection: CallPeerConnection) {
            disconnected += connection
            state.value = state.value.copy(connections = state.value.connections - connection)
        }

        override suspend fun stop() {
            state.value = CallManagerState()
        }
    }

    private companion object {
        val peer = CallPeerConnection("connection", "remote-session")
        val readyPresence = CallPresence(
            connectionId = 1,
            isFocused = true,
            participants = emptyList(),
            isCallInProgress = false,
        )
        val joined = CallRoomSnapshot(
            "call",
            "local-session",
            listOf(CallRoomConnection(peer, true)),
            listOf(CallIceServer(listOf("turn:example.test"), "user", "password")),
            setOf("me", "peer"),
        )
    }
}
