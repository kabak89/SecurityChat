package com.security.chat.multiplatform.features.call.domain.entity

/**
 * A media signal for one connection attempt, with the remote peer identified by connection.
 * Transport envelopes, routing and serialization belong to the data layer.
 */
public sealed interface CallSignal {

    public val connection: CallPeerConnection

    public data class Offer(
        override val connection: CallPeerConnection,
        val sdp: String,
    ) : CallSignal

    public data class Answer(
        override val connection: CallPeerConnection,
        val sdp: String,
    ) : CallSignal

    public data class IceCandidate(
        override val connection: CallPeerConnection,
        val candidate: String,
        val sdpMid: String?,
        val sdpMLineIndex: Int?,
    ) : CallSignal

    public data class IceGatheringComplete(
        override val connection: CallPeerConnection,
    ) : CallSignal
}
