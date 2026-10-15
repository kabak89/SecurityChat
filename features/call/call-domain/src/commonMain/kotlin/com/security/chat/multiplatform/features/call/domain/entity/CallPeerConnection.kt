package com.security.chat.multiplatform.features.call.domain.entity

/**
 * Identifies one connection attempt to a remote participant session, not a user or chat.
 * Both peers share the connection id but reference each other's participant session id.
 * Reconnecting uses a new globally unique id so delayed signals cannot affect the new connection.
 */
public data class CallPeerConnection(
    val id: String,
    val participantSessionId: String,
)
