package com.security.chat.multiplatform.features.mesh.data

import com.security.chat.multiplatform.features.mesh.domain.LogManager
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshPeer

internal actual class BluetoothManagerImpl actual constructor(logManager: LogManager) :
    BluetoothManagerBase() {

    override fun start() = Unit

    override suspend fun sendFrame(peer: MeshPeer, payload: ByteArray) {
        throw UnsupportedOperationException("Bluetooth transport is unavailable on JVM")
    }
}
