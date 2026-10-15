package com.security.chat.multiplatform.features.call.domain.entity

public data class CallManagerState(
    val isStarted: Boolean = false,
    val isMicrophoneEnabled: Boolean = false,
    val connections: Map<CallPeerConnection, CallConnectionState> = emptyMap(),
)
