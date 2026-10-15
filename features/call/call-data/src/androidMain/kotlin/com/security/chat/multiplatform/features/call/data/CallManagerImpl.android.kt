package com.security.chat.multiplatform.features.call.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.call.domain.CallManager
import com.security.chat.multiplatform.features.call.domain.entity.CallConnectionState
import com.security.chat.multiplatform.features.call.domain.entity.CallIceServer
import com.security.chat.multiplatform.features.call.domain.entity.CallManagerState
import com.security.chat.multiplatform.features.call.domain.entity.CallPeerConnection
import com.security.chat.multiplatform.features.call.domain.entity.CallSignal
import com.security.chat.multiplatform.features.call.domain.entity.errors.CallMediaError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.webrtc.AddIceObserver
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.audio.JavaAudioDeviceModule
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.milliseconds

internal actual class CallManagerImpl actual constructor(
    dispatcherProvider: DispatcherProviderInterface,
) : CallManager, KoinComponent {

    private val context: Context by inject()
    private val dispatcher = dispatcherProvider.Default.limitedParallelism(1)
    private val mutex = Mutex()
    private val state = MutableStateFlow(CallManagerState())
    private val generation = MutableStateFlow(0L)
    private val signals = MutableSharedFlow<SignalEmission>(extraBufferCapacity = 128)
    private val peers = mutableMapOf<CallPeerConnection, Peer>()
    private val usedConnectionIds = mutableSetOf<String>()
    private var callbackScope: CoroutineScope? = null
    private var factory: PeerConnectionFactory? = null
    private var audioDevice: JavaAudioDeviceModule? = null
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var iceServers = emptyList<PeerConnection.IceServer>()
    private var audioManager: AudioManager? = null
    private var audioFocus: AudioFocusRequest? = null
    private var previousAudioMode: Int? = null

    override fun getStateFlow(): StateFlow<CallManagerState> = state.asStateFlow()

    override fun getOutgoingSignalsFlow(): Flow<CallSignal> =
        signals.filter {
            val connectionState = state.value.connections[it.signal.connection]
            it.generation == generation.value && connectionState != null &&
                    connectionState != CallConnectionState.Failed &&
                    connectionState != CallConnectionState.Closed
        }.map { it.signal }

    override suspend fun start(iceServers: List<CallIceServer>) {
        withContext(dispatcher) {
            mutex.withLock {
                check(!state.value.isStarted) { "Call manager is already started" }
                val appContext = context.applicationContext
                if (appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    throw CallMediaError("Microphone permission is required")
                }
                try {
                    initializeWebRtc(appContext)
                    generation.value += 1
                    val scope = CoroutineScope(SupervisorJob() + dispatcher)
                    callbackScope = scope
                    configureAudio(appContext, scope, generation.value)
                    this@CallManagerImpl.iceServers = iceServers.map { server ->
                        PeerConnection.IceServer.builder(server.urls)
                            .setUsername(server.username.orEmpty())
                            .setPassword(server.credential.orEmpty())
                            .createIceServer()
                    }
                    audioDevice = JavaAudioDeviceModule.builder(appContext)
                        .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                        .createAudioDeviceModule()
                    factory = PeerConnectionFactory.builder()
                        .setAudioDeviceModule(checkNotNull(audioDevice))
                        .createPeerConnectionFactory()
                    audioSource = checkNotNull(factory).createAudioSource(MediaConstraints())
                    audioTrack = checkNotNull(factory).createAudioTrack(
                        "call-audio", checkNotNull(audioSource),
                    ).also { it.setEnabled(true) }
                    state.value = CallManagerState(isStarted = true, isMicrophoneEnabled = true)
                } catch (error: Throwable) {
                    releaseResources()
                    throw error
                }
            }
        }
    }

    override suspend fun connect(connection: CallPeerConnection, isInitiator: Boolean) {
        withContext(dispatcher) {
            mutex.withLock {
                check(state.value.isStarted) { "Call manager is not started" }
                if (connection in state.value.connections) return@withLock
                check(signals.subscriptionCount.value > 0) { "Collect outgoing signals first" }
                check(usedConnectionIds.add(connection.id)) { "Connection id is already used" }
                val token = Any()
                val scope = checkNotNull(callbackScope)
                val currentFactory = checkNotNull(factory)
                val currentAudioTrack = checkNotNull(audioTrack)
                val configuration = PeerConnection.RTCConfiguration(iceServers).apply {
                    sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                }
                updateConnection(connection, CallConnectionState.Connecting)
                runPeerOperation(
                    onFailure = { error ->
                        failConnection(connection, peers[connection], error)
                    },
                ) {
                    val rtc = currentFactory.createPeerConnection(
                        configuration, observer(connection, token, scope),
                    ) ?: throw CallMediaError("Failed to create peer connection")
                    peers[connection] = Peer(rtc, token, isInitiator)
                    rtc.addTrack(currentAudioTrack, listOf("call-audio-stream"))
                        ?: throw CallMediaError("Failed to add audio track")
                    if (isInitiator) {
                        val offer = rtc.createDescription(isOffer = true)
                        rtc.applyDescription(offer, local = true)
                        emitSignal(CallSignal.Offer(connection, offer.description))
                    }
                }
            }
        }
    }

    override suspend fun handleSignal(signal: CallSignal) {
        withContext(dispatcher) {
            mutex.withLock {
                val peer = peers[signal.connection]
                if (peer == null || peer.failed) {
                    Log.e("Ignoring signal for inactive connection ${signal.connection.id}")
                    return@withLock
                }
                runPeerOperation(
                    onFailure = { error ->
                        failConnection(signal.connection, peer, error)
                    },
                ) {
                    when (signal) {
                        is CallSignal.Offer -> {
                            check(!peer.isInitiator) { "An initiator cannot accept an offer" }
                            if (peer.remoteSdp == signal.sdp) return@withLock
                            check(peer.remoteSdp == null) {
                                "Use a new connection for renegotiation"
                            }
                            applyRemoteDescription(peer, SessionDescription.Type.OFFER, signal.sdp)
                            val answer = peer.rtc.createDescription(isOffer = false)
                            peer.rtc.applyDescription(answer, local = true)
                            emitSignal(CallSignal.Answer(signal.connection, answer.description))
                        }

                        is CallSignal.Answer -> {
                            check(peer.isInitiator) { "Only an initiator can accept an answer" }
                            if (peer.remoteSdp == signal.sdp) return@withLock
                            check(peer.remoteSdp == null) {
                                "Use a new connection for renegotiation"
                            }
                            applyRemoteDescription(peer, SessionDescription.Type.ANSWER, signal.sdp)
                        }

                        is CallSignal.IceCandidate -> {
                            require(signal.sdpMid != null || signal.sdpMLineIndex != null)
                            val candidate = IceCandidate(
                                signal.sdpMid, signal.sdpMLineIndex ?: -1, signal.candidate,
                            )
                            if (peer.remoteSdp == null) {
                                check(peer.pendingIce.size < MAX_PENDING_ICE) {
                                    "Too many pending ICE candidates"
                                }
                                peer.pendingIce += candidate
                            } else {
                                peer.rtc.applyIce(candidate)
                            }
                        }

                        is CallSignal.IceGatheringComplete -> {
                            /** Native WebRTC does not require a remote gathering-complete marker. */
                        }
                    }
                }
            }
        }
    }

    override suspend fun failConnection(connection: CallPeerConnection, error: Throwable) {
        withContext(dispatcher) {
            mutex.withLock {
                if (connection !in state.value.connections) {
                    Log.e("Ignoring failure for inactive connection ${connection.id}")
                    return@withLock
                }
                failConnection(connection, peers[connection], error)
            }
        }
    }

    override suspend fun disconnect(connection: CallPeerConnection) {
        withContext(dispatcher) {
            mutex.withLock {
                val peer = peers.remove(connection)
                state.update { it.copy(connections = it.connections - connection) }
                release { peer?.rtc?.dispose() }
            }
        }
    }

    override suspend fun stop() {
        withContext(NonCancellable + dispatcher) {
            mutex.withLock { releaseResources() }
        }
    }

    private suspend fun applyRemoteDescription(
        peer: Peer,
        type: SessionDescription.Type,
        sdp: String,
    ) {
        peer.rtc.applyDescription(SessionDescription(type, sdp), local = false)
        peer.remoteSdp = sdp
        for (candidate in peer.pendingIce) peer.rtc.applyIce(candidate)
        peer.pendingIce.clear()
    }

    private fun configureAudio(appContext: Context, scope: CoroutineScope, callGeneration: Long) {
        val manager = appContext.getSystemService(AudioManager::class.java)
        audioManager = manager
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setOnAudioFocusChangeListener(
                { change ->
                    if (change < 0) {
                        scope.launch {
                            mutex.withLock {
                                if (generation.value == callGeneration) releaseResources()
                            }
                        }
                    }
                },
                Handler(Looper.getMainLooper()),
            )
            .build()
        audioFocus = request
        if (manager.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            throw CallMediaError("Audio focus is unavailable")
        }
        previousAudioMode = manager.mode
        manager.mode = AudioManager.MODE_IN_COMMUNICATION
    }

    private fun releaseResources() {
        generation.value += 1
        callbackScope?.cancel()
        callbackScope = null
        state.value = CallManagerState()
        val connections = peers.values.toList()
        peers.clear()
        usedConnectionIds.clear()
        connections.forEach { release { it.rtc.dispose() } }
        release { audioTrack?.setEnabled(false) }
        release { audioTrack?.dispose() }
        audioTrack = null
        release { audioSource?.dispose() }
        audioSource = null
        release { factory?.dispose() }
        factory = null
        release { audioDevice?.release() }
        audioDevice = null
        iceServers = emptyList()
        release { audioFocus?.let { audioManager?.abandonAudioFocusRequest(it) } }
        audioFocus = null
        release { previousAudioMode?.let { audioManager?.mode = it } }
        previousAudioMode = null
        audioManager = null
    }

    private fun release(block: () -> Unit) {
        try {
            block()
        } catch (error: Throwable) {
            Log.e(error, "Failed to release call resource")
        }
    }

    private fun updateConnection(connection: CallPeerConnection, value: CallConnectionState) {
        state.update { it.copy(connections = it.connections + (connection to value)) }
    }

    private fun failConnection(connection: CallPeerConnection, peer: Peer?, error: Throwable) {
        Log.e(error, "WebRTC connection ${connection.id} failed")
        if (peer != null && !peer.failed) {
            peer.failed = true
            peer.pendingIce.clear()
            release { peer.rtc.close() }
        }
        updateConnection(connection, CallConnectionState.Failed)
    }

    private fun emitSignal(signal: CallSignal) {
        if (signals.subscriptionCount.value == 0 ||
            !signals.tryEmit(SignalEmission(generation.value, signal))
        ) {
            throw CallMediaError("Outgoing call signal could not be delivered to its collector")
        }
    }

    private fun observer(
        connection: CallPeerConnection,
        token: Any,
        scope: CoroutineScope,
    ): PeerConnection.Observer =
        object : PeerConnection.Observer {
            private fun dispatch(block: () -> Unit) {
                scope.launch {
                    mutex.withLock {
                        val peer = peers[connection]
                        if (peer == null || peer.token !== token || peer.failed) return@withLock
                        runPeerOperation(
                            onFailure = { error ->
                                failConnection(connection, peer, error)
                            },
                        ) {
                            block()
                        }
                    }
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                dispatch {
                    val value = when (newState) {
                        PeerConnection.PeerConnectionState.NEW -> CallConnectionState.New
                        PeerConnection.PeerConnectionState.CONNECTING ->
                            CallConnectionState.Connecting

                        PeerConnection.PeerConnectionState.CONNECTED ->
                            CallConnectionState.Connected

                        PeerConnection.PeerConnectionState.DISCONNECTED ->
                            CallConnectionState.Disconnected

                        PeerConnection.PeerConnectionState.FAILED -> CallConnectionState.Failed
                        PeerConnection.PeerConnectionState.CLOSED -> CallConnectionState.Closed
                    }
                    if (value == CallConnectionState.Failed || value == CallConnectionState.Closed) {
                        failConnection(
                            connection, checkNotNull(peers[connection]),
                            CallMediaError("WebRTC connection entered $newState"),
                        )
                    } else {
                        updateConnection(connection, value)
                    }
                }
            }

            override fun onIceCandidate(candidate: IceCandidate) {
                dispatch {
                    emitSignal(
                        CallSignal.IceCandidate(
                            connection, candidate.sdp, candidate.sdpMid, candidate.sdpMLineIndex,
                        ),
                    )
                }
            }

            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {
                if (newState == PeerConnection.IceGatheringState.COMPLETE) {
                    dispatch { emitSignal(CallSignal.IceGatheringComplete(connection)) }
                }
            }

            override fun onSignalingChange(newState: PeerConnection.SignalingState) = Unit
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) = Unit
            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) = Unit
            override fun onAddStream(stream: MediaStream) = Unit
            override fun onRemoveStream(stream: MediaStream) = Unit
            override fun onDataChannel(channel: DataChannel) = Unit
            override fun onRenegotiationNeeded() = Unit
        }

    private class Peer(
        val rtc: PeerConnection,
        val token: Any,
        val isInitiator: Boolean,
        val pendingIce: MutableList<IceCandidate> = mutableListOf(),
        var remoteSdp: String? = null,
        var failed: Boolean = false,
    )

    private data class SignalEmission(val generation: Long, val signal: CallSignal)

    private companion object {
        val initializationLock = Any()
        var initialized = false

        fun initializeWebRtc(context: Context) {
            synchronized(initializationLock) {
                if (!initialized) {
                    PeerConnectionFactory.initialize(
                        PeerConnectionFactory.InitializationOptions.builder(context)
                            .createInitializationOptions(),
                    )
                    initialized = true
                }
            }
        }
    }
}

private suspend fun PeerConnection.createDescription(isOffer: Boolean): SessionDescription =
    withTimeout(SDP_TIMEOUT_MS.milliseconds) {
        suspendCancellableCoroutine { continuation ->
            val observer = object : SdpObserver {
                override fun onCreateSuccess(description: SessionDescription) {
                    continuation.resume(description)
                }

                override fun onCreateFailure(error: String) {
                    continuation.resumeWithException(CallMediaError("SDP creation failed: $error"))
                }

                override fun onSetSuccess() = Unit
                override fun onSetFailure(error: String) = Unit
            }
            if (isOffer) {
                createOffer(observer, MediaConstraints())
            } else {
                createAnswer(observer, MediaConstraints())
            }
        }
    }

private suspend fun PeerConnection.applyDescription(sdp: SessionDescription, local: Boolean) {
    withTimeout(SDP_TIMEOUT_MS.milliseconds) {
        suspendCancellableCoroutine { continuation ->
            val observer = object : SdpObserver {
                override fun onSetSuccess() {
                    continuation.resume(Unit)
                }

                override fun onSetFailure(error: String) {
                    continuation.resumeWithException(
                        CallMediaError("SDP application failed: $error"),
                    )
                }

                override fun onCreateSuccess(description: SessionDescription) = Unit
                override fun onCreateFailure(error: String) = Unit
            }
            if (local) setLocalDescription(observer, sdp) else setRemoteDescription(observer, sdp)
        }
    }
}

private suspend fun PeerConnection.applyIce(candidate: IceCandidate) {
    withTimeout(SDP_TIMEOUT_MS.milliseconds) {
        suspendCancellableCoroutine { continuation ->
            addIceCandidate(
                candidate,
                object : AddIceObserver {
                    override fun onAddSuccess() {
                        continuation.resume(Unit)
                    }

                    override fun onAddFailure(error: String) {
                        continuation.resumeWithException(
                            CallMediaError("ICE application failed: $error"),
                        )
                    }
                },
            )
        }
    }
}

private const val SDP_TIMEOUT_MS = 10_000L
private const val MAX_PENDING_ICE = 256
