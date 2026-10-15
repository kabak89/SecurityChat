package com.security.chat.multiplatform.features.call.domain

import com.security.chat.multiplatform.features.call.domain.entity.CallIceServer
import com.security.chat.multiplatform.features.call.domain.entity.CallManagerState
import com.security.chat.multiplatform.features.call.domain.entity.CallPeerConnection
import com.security.chat.multiplatform.features.call.domain.entity.CallSignal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Owns local audio and peer connections for one call at a time.
 * The caller handles server communication, microphone permission and peer selection.
 * Implementations must serialize commands and WebRTC callbacks internally.
 */
public interface CallManager {

    public fun getStateFlow(): StateFlow<CallManagerState>

    /**
     * Collect once before starting. Signals target the remote participant in their connection.
     * Forward them through the signaling transport; old signals must not be replayed after stop.
     */
    public fun getOutgoingSignalsFlow(): Flow<CallSignal>

    /** Starts local audio after explicit user consent. Requires the manager to be stopped. */
    public suspend fun start(iceServers: List<CallIceServer>)

    /**
     * Creates a peer connection after start. Exactly one peer must be the initiator.
     * The initiator emits an offer; the other peer waits for it through handleSignal.
     * Returning does not mean that the audio connection is established; observe the state flow.
     * Peer creation and SDP errors mark only this connection Failed without throwing.
     * Global audio errors and external coroutine cancellation still propagate to the caller.
     */
    public suspend fun connect(connection: CallPeerConnection, isInitiator: Boolean)

    /**
     * Applies a remote signal to a connection previously registered through connect.
     * The implementation generates answers and buffers ICE until remote SDP is applied.
     * Signals for closed or unknown connections must be logged and discarded.
     * SDP and ICE errors, including operation timeouts, mark only this connection Failed.
     */
    public suspend fun handleSignal(signal: CallSignal)

    /**
     * Marks a registered connection Failed and closes its media after a signaling rejection.
     * Keeps its terminal state until disconnect or stop; outgoing signals must be discarded.
     */
    public suspend fun failConnection(connection: CallPeerConnection, error: Throwable)

    /** Closes and removes one peer connection. Safe to call repeatedly for the same connection. */
    public suspend fun disconnect(connection: CallPeerConnection)

    /**
     * Disables audio, releases all media resources and resets state, including after a failed start.
     * Safe to call repeatedly. The manager can be started again after stop completes.
     */
    public suspend fun stop()
}
