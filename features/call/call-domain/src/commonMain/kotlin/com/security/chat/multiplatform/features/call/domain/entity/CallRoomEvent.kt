package com.security.chat.multiplatform.features.call.domain.entity

public data class CallPresence(
    val connectionId: Long?,
    val isFocused: Boolean,
    val participants: List<CallParticipant>,
    val isCallInProgress: Boolean,
)

public data class CallRoomConnection(
    val connection: CallPeerConnection,
    val isInitiator: Boolean,
)

public data class CallRoomSnapshot(
    val callId: String?,
    val participantSessionId: String?,
    val connections: List<CallRoomConnection>,
    val iceServers: List<CallIceServer>,
    val participantUserIds: Set<String>,
)

public sealed interface CallRoomEvent {
    public data class Snapshot(val snapshot: CallRoomSnapshot) : CallRoomEvent
    public data class Joined(val requestId: String, val snapshot: CallRoomSnapshot) : CallRoomEvent
    public data class Signal(val callId: String, val signal: CallSignal) : CallRoomEvent
    public data class SignalRejected(val connection: CallPeerConnection, val code: String) :
        CallRoomEvent

    public data class Revoked(val reason: String) : CallRoomEvent
    public data class Error(val code: String, val message: String) : CallRoomEvent
}
