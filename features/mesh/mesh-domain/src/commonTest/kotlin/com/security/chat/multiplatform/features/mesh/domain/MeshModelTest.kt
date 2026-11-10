package com.security.chat.multiplatform.features.mesh.domain

import com.security.chat.multiplatform.features.mesh.domain.entity.MeshIncomingPacket
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshLog
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshPeer
import com.security.chat.multiplatform.features.mesh.domain.repo.MeshRepo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MeshModelTest {

    private val messages = mutableListOf<String>()
    private val logManager = object : LogManager {
        override fun d(message: () -> String) {
            messages += message()
        }

        override fun e(message: String, error: Throwable?) = Unit
        override fun getLogsFlow(): Flow<List<MeshLog>> = emptyFlow()
    }

    @Test
    fun `concurrent calls share a sync and new sessions are picked up before it completes`() =
        runTest {
            val firstPeer = MeshPeer(sessionId = "first")
            val disconnectedPeer = MeshPeer(sessionId = "disconnected")
            val newPeer = MeshPeer(sessionId = "new")
            val releaseFirstPeer = CompletableDeferred<Unit>()
            val repo = FakeMeshRepo { peer ->
                if (peer == firstPeer) releaseFirstPeer.await()
            }
            val model = startModel(repo)
            repo.readyPeers.value = listOf(firstPeer, disconnectedPeer)

            val firstCall = async { model.sync() }
            runCurrent()
            val secondCall = async { model.sync() }
            runCurrent()

            assertEquals(listOf(firstPeer to "mesh sync"), repo.attemptedSends)
            assertFalse(secondCall.isCompleted)

            firstCall.cancelAndJoin()
            repo.readyPeers.value = listOf(firstPeer, newPeer)
            releaseFirstPeer.complete(Unit)
            secondCall.await()

            assertEquals(
                listOf(firstPeer to "mesh sync", newPeer to "mesh sync"),
                repo.attemptedSends,
            )
        }

    @Test
    fun `a failed peer does not prevent syncing the other ready peers`() = runTest {
        val firstPeer = MeshPeer(sessionId = "first")
        val secondPeer = MeshPeer(sessionId = "second")
        val repo = FakeMeshRepo { peer ->
            if (peer == firstPeer) error("Peer disconnected during sync")
        }
        val model = startModel(repo)
        repo.readyPeers.value = listOf(firstPeer, secondPeer)

        model.sync()

        assertEquals(
            listOf(firstPeer to "mesh sync", secondPeer to "mesh sync"),
            repo.attemptedSends,
        )
    }

    @Test
    fun `sync waits until a peer is ready`() = runTest {
        val peer = MeshPeer(sessionId = "nearby")
        val repo = FakeMeshRepo { }
        val model = startModel(repo)

        val call = async { model.sync() }
        runCurrent()

        assertFalse(call.isCompleted)
        assertTrue(repo.attemptedSends.isEmpty())

        repo.readyPeers.value = listOf(peer)
        call.await()

        assertEquals(listOf(peer to "mesh sync"), repo.attemptedSends)
    }

    @Test
    fun `sync stops waiting after 30 seconds when no peer is ready`() = runTest {
        val repo = FakeMeshRepo { }
        val model = startModel(repo)

        model.sync()

        assertEquals(30_000L, currentTime)
        assertTrue(repo.attemptedSends.isEmpty())
    }

    @Test
    fun `incoming packets are logged only while the model is started`() = runTest {
        val repo = FakeMeshRepo { }
        val model = MeshModelImpl(
            meshRepo = repo,
            dispatcher = StandardTestDispatcher(testScheduler),
            logManager = logManager,
        )
        val peer = MeshPeer(sessionId = "nearby")
        repo.incomingPackets.send(MeshIncomingPacket(peer = peer, text = "buffered"))
        runCurrent()
        assertTrue(messages.isEmpty())

        val job = model.start(parentScope = backgroundScope)
        runCurrent()
        assertEquals(listOf("Mesh packet from session nearby: buffered"), messages)

        job.cancelAndJoin()
        repo.incomingPackets.send(MeshIncomingPacket(peer = peer, text = "after cancellation"))
        runCurrent()
        assertEquals(listOf("Mesh packet from session nearby: buffered"), messages)
    }

    private fun TestScope.startModel(repo: MeshRepo): MeshModel {
        val model = MeshModelImpl(
            meshRepo = repo,
            dispatcher = StandardTestDispatcher(testScheduler),
            logManager = logManager,
        )
        model.start(parentScope = backgroundScope)
        return model
    }

    private class FakeMeshRepo(
        private val onSync: suspend (MeshPeer) -> Unit,
    ) : MeshRepo {

        val readyPeers = MutableStateFlow<List<MeshPeer>>(emptyList())
        val incomingPackets = Channel<MeshIncomingPacket>(Channel.UNLIMITED)
        val attemptedSends = mutableListOf<Pair<MeshPeer, String>>()

        override fun getReadyPeersFlow(): StateFlow<List<MeshPeer>> = readyPeers

        override fun getIncomingPacketsFlow(): Flow<MeshIncomingPacket> =
            incomingPackets.receiveAsFlow()

        override suspend fun send(peer: MeshPeer, text: String) {
            attemptedSends += peer to text
            onSync(peer)
        }
    }
}
