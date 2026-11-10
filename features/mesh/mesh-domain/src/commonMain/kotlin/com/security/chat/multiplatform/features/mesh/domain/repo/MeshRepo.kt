package com.security.chat.multiplatform.features.mesh.domain.repo

import com.security.chat.multiplatform.features.mesh.domain.entity.MeshIncomingPacket
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshPeer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

public interface MeshRepo {

    public fun getReadyPeersFlow(): StateFlow<List<MeshPeer>>

    /** Delivers packets to a single consumer. */
    public fun getIncomingPacketsFlow(): Flow<MeshIncomingPacket>

    public suspend fun send(peer: MeshPeer, text: String)
}
