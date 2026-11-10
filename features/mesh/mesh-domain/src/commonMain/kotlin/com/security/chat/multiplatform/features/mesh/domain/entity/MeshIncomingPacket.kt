package com.security.chat.multiplatform.features.mesh.domain.entity

public data class MeshIncomingPacket(
    public val peer: MeshPeer,
    public val text: String,
)
