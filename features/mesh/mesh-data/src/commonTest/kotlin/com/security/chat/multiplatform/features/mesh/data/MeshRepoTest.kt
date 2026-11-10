package com.security.chat.multiplatform.features.mesh.data

import com.security.chat.multiplatform.features.mesh.domain.LogManager
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshIncomingPacket
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshLog
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshPeer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MeshRepoTest {

    private val logManager = object : LogManager {
        override fun d(message: () -> String) = Unit
        override fun e(message: String, error: Throwable?) = Unit
        override fun getLogsFlow(): Flow<List<MeshLog>> = emptyFlow()
    }

    @Test
    fun `fragmented UTF-8 is reassembled before the consumer starts`() = runTest {
        val frameSizes = mutableListOf<Int>()
        val loopback = TestBluetoothManager { peer, frame ->
            frameSizes += frame.size
            assertTrue(handleIncomingPacket(peer, frame))
            frame.fill(0)
        }
        val repo = MeshRepoImpl(bluetoothManager = loopback, logManager = logManager)
        val peer = MeshPeer(sessionId = "session")
        val text = "123456789012🙂 Привет по BLE"

        repo.send(peer = peer, text = text)

        assertTrue(frameSizes.size > 1)
        assertTrue(frameSizes.all { it <= MAX_MESH_PAYLOAD_BYTES })
        assertEquals(
            MeshIncomingPacket(peer = peer, text = text),
            repo.getIncomingPacketsFlow().first(),
        )
    }

    @Test
    fun `concurrent messages wait for all frames of the active message`() = runTest {
        val firstFrameStarted = CompletableDeferred<Unit>()
        val releaseFirstFrame = CompletableDeferred<Unit>()
        val frames = mutableListOf<ByteArray>()
        val loopback = TestBluetoothManager { peer, frame ->
            frames += frame
            if (frames.size == 1) {
                firstFrameStarted.complete(Unit)
                releaseFirstFrame.await()
            }
            assertTrue(handleIncomingPacket(peer, frame))
        }
        val repo = MeshRepoImpl(bluetoothManager = loopback, logManager = logManager)
        val peer = MeshPeer(sessionId = "session")
        val longText = "A long message ".repeat(5)
        val shortText = "short"

        val first = async { repo.send(peer = peer, text = longText) }
        firstFrameStarted.await()
        val second = async { repo.send(peer = peer, text = shortText) }
        runCurrent()
        assertEquals(1, frames.size)

        releaseFirstFrame.complete(Unit)
        first.await()
        second.await()

        assertTrue(frames.last().contentEquals(shortText.encodeToByteArray()))
        assertEquals(
            listOf(longText, shortText),
            repo.getIncomingPacketsFlow().take(2).toList().map { it.text },
        )
    }

    @Test
    fun `incomplete messages are discarded on a missing frame or disconnect`() = runTest {
        val frames = mutableListOf<ByteArray>()
        val manager = TestBluetoothManager { _, frame -> frames += frame }
        val repo = MeshRepoImpl(bluetoothManager = manager, logManager = logManager)
        val peer = MeshPeer(sessionId = "session")
        val text = "A complete message ".repeat(4)
        manager.setReadyPeers(listOf(peer))
        repo.send(peer = peer, text = text)

        assertTrue(manager.handleIncomingPacket(peer, frames.first()))
        assertFalse(manager.handleIncomingPacket(peer, frames[2]))
        assertFalse(manager.handleIncomingPacket(peer, frames[1]))

        assertTrue(manager.handleIncomingPacket(peer, frames.first()))
        manager.setReadyPeers(emptyList())
        assertFalse(manager.handleIncomingPacket(peer, frames[1]))

        manager.setReadyPeers(listOf(peer))
        frames.forEach { assertTrue(manager.handleIncomingPacket(peer, it)) }
        assertEquals(text, repo.getIncomingPacketsFlow().first().text)
    }

    private class TestBluetoothManager(
        private val onSend: suspend TestBluetoothManager.(MeshPeer, ByteArray) -> Unit,
    ) : BluetoothManagerBase() {

        override fun start() = Unit

        override suspend fun sendFrame(peer: MeshPeer, payload: ByteArray) {
            onSend(peer, payload)
        }

        fun setReadyPeers(peers: List<MeshPeer>) {
            updateReadyPeers(peers)
        }
    }
}
