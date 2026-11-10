package com.security.chat.multiplatform.features.mesh.data

import com.security.chat.multiplatform.features.mesh.domain.LogManager
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshIncomingPacket
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshPeer
import com.security.chat.multiplatform.features.mesh.domain.repo.MeshRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

internal class MeshRepoImpl(
    private val bluetoothManager: BluetoothManager,
    private val logManager: LogManager,
) : MeshRepo {

    override fun getReadyPeersFlow(): StateFlow<List<MeshPeer>> =
        bluetoothManager.getReadyPeersFlow()

    override fun getIncomingPacketsFlow(): Flow<MeshIncomingPacket> {
        return bluetoothManager.getIncomingPacketsFlow().map { packet ->
            val text = packet.payload.decodeToString()
            logManager.d { "MeshRepoImpl getIncomingPacketsFlow. info = $text" }
            MeshIncomingPacket(peer = packet.peer, text = text)
        }
    }

    override suspend fun send(peer: MeshPeer, text: String) {
        logManager.d { "MeshRepoImpl send" }
        bluetoothManager.send(peer = peer, payload = text.encodeToByteArray())
    }
}
