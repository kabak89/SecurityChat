package com.security.chat.multiplatform.features.call.data.entity

import kotlinx.serialization.Serializable

@Serializable
internal data class CallEnvelope(val type: String, val payload: String)

@Serializable
internal data class CallCommand(
    val requestId: String,
    val chatId: String,
    val callId: String? = null,
    val connectionId: String? = null,
    val participantSessionId: String? = null,
    val signalType: String? = null,
    val sdp: String? = null,
    val candidate: String? = null,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null,
)

@Serializable
internal data class CallSnapshotMessage(
    val chatId: String,
    val callId: String? = null,
    val participantSessionId: String? = null,
    val connections: List<CallConnectionMessage> = emptyList(),
    val iceServers: List<CallIceServerMessage> = emptyList(),
    val requestId: String? = null,
    val participants: List<CallParticipantMessage> = emptyList(),
)

@Serializable
internal data class CallParticipantMessage(
    val participantSessionId: String,
    val userId: String,
)

@Serializable
internal data class CallConnectionMessage(
    val id: String,
    val participantSessionId: String,
    val isInitiator: Boolean,
)

@Serializable
internal data class CallIceServerMessage(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null,
)

@Serializable
internal data class CallSignalMessage(
    val chatId: String,
    val callId: String,
    val connectionId: String,
    val participantSessionId: String,
    val signalType: String,
    val sdp: String? = null,
    val candidate: String? = null,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null,
)

@Serializable
internal data class CallErrorMessage(
    val code: String,
    val message: String,
    val requestId: String? = null,
)

@Serializable
internal data class CallAcknowledgement(val requestId: String, val command: String)

@Serializable
internal data class CallRevokedMessage(val chatId: String, val reason: String)
