package com.security.chat.multiplatform.features.mesh.domain.entity

/** Identifies one ready session. Reconnecting to the same device creates a new session ID. */
public data class MeshPeer(public val sessionId: String)
