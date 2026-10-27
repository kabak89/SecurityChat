package com.security.chat.multiplatform.features.call.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.call.domain.entity.CallConnectionState
import com.security.chat.multiplatform.features.call.domain.entity.CallManagerState
import com.security.chat.multiplatform.features.call.domain.entity.CallPeerConnection
import com.security.chat.multiplatform.features.call.domain.entity.CallPresence
import com.security.chat.multiplatform.features.call.domain.entity.CallRoomEvent
import com.security.chat.multiplatform.features.call.domain.entity.CallRoomSnapshot
import com.security.chat.multiplatform.features.call.domain.entity.CallStatus
import com.security.chat.multiplatform.features.call.domain.entity.errors.CallMediaError
import com.security.chat.multiplatform.features.call.domain.entity.errors.CallSignalingError
import com.security.chat.multiplatform.features.call.domain.repo.CallRepo
import com.security.chat.multiplatform.features.call.domain.repo.CallSession
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import ru.kode.remo.Task0
import kotlin.time.Duration.Companion.milliseconds

public interface CallModel : ScopedModel {
    public val startCall: Task0<Unit>
    public val endCall: Task0<Unit>

    public fun setChatId(id: String)
    public fun setFocused(isFocused: Boolean)
    public fun getPresenceFlow(): Flow<CallPresence>
    public fun getCallStatusFlow(): Flow<CallStatus>
    public fun getCallManagerStateFlow(): StateFlow<CallManagerState>
}

internal class CallModelImpl(
    private val callRepo: CallRepo,
    private val callManager: CallManager,
    dispatcherProvider: DispatcherProviderInterface,
) : CallModel, BaseModel(dispatcher = dispatcherProvider.Default) {
    private val state = MutableStateFlow(State())
    private val presence = MutableStateFlow(
        CallPresence(
            connectionId = null,
            isFocused = false,
            participants = emptyList(),
            isCallInProgress = false,
        ),
    )
    private val runningCall = MutableStateFlow<Job?>(null)

    override fun onPostStart() {
        super.onPostStart()
        scope.launch {
            state.flatMapLatest { current ->
                if (current.chatId != null && current.isFocused) {
                    callRepo.getPresenceFlow(current.chatId).catch { error ->
                        Log.e(error, "Call presence subscription failed")
                        emit(
                            CallPresence(
                                connectionId = null,
                                isFocused = false,
                                participants = emptyList(),
                                isCallInProgress = false,
                            ),
                        )
                    }
                } else {
                    flowOf(
                        CallPresence(
                            connectionId = null,
                            isFocused = false,
                            participants = emptyList(),
                            isCallInProgress = false,
                        ),
                    )
                }
            }.collect { presence.value = it }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) {
                    runningCall.value?.cancelAndJoin()
                    callManager.stop()
                }
            }
        }
    }

    override val startCall: Task0<Unit> = task { ->
        val job = currentCoroutineContext().job
        if (runningCall.compareAndSet(null, job)) {
            try {
                val current = state.value
                val chatId = checkNotNull(current.chatId)
                check(current.isFocused) { "The call screen must be focused" }
                coroutineScope {
                    val initialPresence = withTimeout(JOIN_TIMEOUT_MS) {
                        presence.first { it.isFocused && it.connectionId != null }
                    }
                    launch {
                        presence.first {
                            !it.isFocused || it.connectionId != initialPresence.connectionId
                        }
                        throw CallSignalingError("FOCUS_LOST")
                    }
                    callRepo.withCallSession(chatId) { session -> runCall(session) }
                }
            } catch (error: TimeoutCancellationException) {
                throw CallSignalingError("TIMEOUT", cause = error)
            } finally {
                withContext(NonCancellable) {
                    callManager.stop()
                    runningCall.compareAndSet(job, null)
                }
            }
        }
    }

    override val endCall: Task0<Unit> = task { -> runningCall.value?.cancelAndJoin() }

    private suspend fun runCall(session: CallSession) {
        coroutineScope {
            var callId: String? = null
            val connections = MutableStateFlow<Set<CallPeerConnection>>(emptySet())
            try {
                val incoming = session.events.produceIn(this)
                val joined = withTimeout(JOIN_TIMEOUT_MS.milliseconds) {
                    /** Commands are sent only after the subscription's initial server snapshot. */
                    while (true) {
                        when (val event = incoming.receive()) {
                            is CallRoomEvent.Snapshot -> break
                            is CallRoomEvent.Error -> throw CallSignalingError(
                                event.code,
                                event.message,
                            )

                            is CallRoomEvent.Revoked -> throw CallSignalingError(event.reason)
                            else -> Unit
                        }
                    }
                    session.join(JOIN_REQUEST_ID)
                    var result: CallRoomSnapshot? = null
                    while (result == null) {
                        when (val event = incoming.receive()) {
                            is CallRoomEvent.Joined -> {
                                if (event.requestId == JOIN_REQUEST_ID) result = event.snapshot
                            }

                            is CallRoomEvent.Error -> throw CallSignalingError(
                                event.code,
                                event.message,
                            )

                            is CallRoomEvent.Revoked -> throw CallSignalingError(event.reason)
                            else -> Unit
                        }
                    }
                    result
                }
                callId = requireNotNull(joined.callId)
                requireNotNull(joined.participantSessionId)
                val activeCallId = callId
                launch(start = CoroutineStart.UNDISPATCHED) {
                    callManager.getOutgoingSignalsFlow().collect { signal ->
                        if (signal.connection in connections.value) {
                            session.sendSignal(activeCallId, signal)
                        }
                    }
                }
                callManager.start(joined.iceServers)
                launch {
                    combine(callManager.getStateFlow(), connections) { media, expected ->
                        when {
                            !media.isStarted -> CallMediaError.Audio("Call audio stopped")
                            expected.isNotEmpty() && expected.all { connection ->
                                when (media.connections[connection]) {
                                    CallConnectionState.Failed, CallConnectionState.Closed -> true
                                    else -> false
                                }
                            } -> CallMediaError.Connection("All peer connections failed")

                            else -> null
                        }
                    }.filterNotNull().first().let { throw it }
                }
                reconcile(joined, connections)
                for (event in incoming) {
                    when (event) {
                        is CallRoomEvent.Snapshot -> {
                            val snapshot = event.snapshot
                            if (snapshot.callId != callId ||
                                snapshot.participantSessionId != joined.participantSessionId
                            ) {
                                throw CallSignalingError("SESSION_ENDED")
                            }
                            reconcile(snapshot, connections)
                        }

                        is CallRoomEvent.Signal -> {
                            if (event.callId == callId &&
                                event.signal.connection in connections.value
                            ) {
                                callManager.handleSignal(event.signal)
                            }
                        }

                        is CallRoomEvent.SignalRejected -> {
                            if (event.connection in connections.value) {
                                val error = CallSignalingError(event.code)
                                if (event.code in PEER_SIGNAL_ERROR_CODES) {
                                    callManager.failConnection(event.connection, error)
                                } else {
                                    throw error
                                }
                            }
                        }

                        is CallRoomEvent.Revoked -> throw CallSignalingError(event.reason)
                        is CallRoomEvent.Joined -> Unit
                        is CallRoomEvent.Error -> throw CallSignalingError(
                            event.code,
                            event.message,
                        )
                    }
                }
                throw CallSignalingError("DISCONNECTED")
            } finally {
                currentCoroutineContext().cancelChildren()
                withContext(NonCancellable) {
                    connections.value = emptySet()
                    callManager.stop()
                    callId?.let {
                        try {
                            withTimeoutOrNull(LEAVE_TIMEOUT_MS) { session.leave(it) }
                        } catch (error: Exception) {
                            Log.e(
                                error,
                                "Could not send call leave; room observer will complete cleanup",
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun reconcile(
        snapshot: CallRoomSnapshot,
        connections: MutableStateFlow<Set<CallPeerConnection>>,
    ) {
        val previous = connections.value
        val next = snapshot.connections.map { it.connection }.toSet()
        connections.value = next
        for (removed in previous - next) callManager.disconnect(removed)
        for (added in snapshot.connections.filter { it.connection !in previous }) {
            callManager.connect(added.connection, added.isInitiator)
        }
    }

    override fun setChatId(id: String) {
        check(state.value.chatId == null)
        state.update { it.copy(chatId = id) }
    }

    override fun setFocused(isFocused: Boolean) {
        state.update { it.copy(isFocused = isFocused) }
        if (!isFocused) {
            presence.value = CallPresence(
                connectionId = null,
                isFocused = false,
                participants = emptyList(),
                isCallInProgress = false,
            )
            runningCall.value?.cancel()
        }
    }

    override fun getPresenceFlow(): Flow<CallPresence> = presence

    override fun getCallStatusFlow(): Flow<CallStatus> =
        combine(runningCall, callManager.getStateFlow()) { job, media ->
            when {
                job == null -> CallStatus.Idle
                media.connections.values.any { it == CallConnectionState.Connected } ->
                    CallStatus.Active

                else -> CallStatus.Connecting
            }
        }.distinctUntilChanged()

    override fun getCallManagerStateFlow(): StateFlow<CallManagerState> = callManager.getStateFlow()

    private data class State(val chatId: String? = null, val isFocused: Boolean = false)
}

private const val JOIN_TIMEOUT_MS = 15_000L
private const val LEAVE_TIMEOUT_MS = 2_000L
private const val JOIN_REQUEST_ID = "join"
private val PEER_SIGNAL_ERROR_CODES = setOf(
    "UNKNOWN_PARTICIPANT", "STALE_CONNECTION", "INVALID_SIGNAL", "INVALID_SIGNAL_STATE",
    "NOT_ELIGIBLE",
)
