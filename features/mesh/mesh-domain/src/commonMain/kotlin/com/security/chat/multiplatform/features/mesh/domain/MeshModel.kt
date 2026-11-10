package com.security.chat.multiplatform.features.mesh.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshPeer
import com.security.chat.multiplatform.features.mesh.domain.repo.MeshRepo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

public interface MeshModel : ScopedModel {

    public suspend fun sync()
}

internal class MeshModelImpl(
    private val meshRepo: MeshRepo,
    dispatcher: CoroutineDispatcher,
    private val logManager: LogManager,
) : MeshModel, BaseModel(dispatcher = dispatcher) {

    private val syncMutex = Mutex()
    private var activeSync: Deferred<Unit>? = null

    override suspend fun sync() {
        val readyPeers = meshRepo.getReadyPeersFlow()
        if (readyPeers.value.isEmpty()) {
            logManager.d { "Mesh sync waiting for a ready BLE peer" }
            val peers = withTimeoutOrNull(READY_PEER_TIMEOUT_MS.milliseconds) {
                readyPeers.first { it.isNotEmpty() }
            }
            if (peers == null) {
                logManager.d { "Mesh sync timed out waiting for a ready BLE peer" }
                return
            }
        }

        val job = syncMutex.withLock {
            activeSync?.takeUnless { it.isCompleted } ?: scope.async {
                logManager.d { "Mesh sync starting (ready peers=${readyPeers.value.size})" }
                syncReadyPeers(readyPeers)
            }.also { activeSync = it }
        }
        job.await()
    }

    override fun onPostStart() {
        super.onPostStart()

        meshRepo.getIncomingPacketsFlow()
            .onEach { packet ->
                logManager.d {
                    "Mesh packet from session ${packet.peer.sessionId}: ${packet.text}"
                }
            }
            .launchIn(scope)
    }

    private suspend fun syncReadyPeers(readyPeers: StateFlow<List<MeshPeer>>) {
        val attemptedPeers = mutableSetOf<MeshPeer>()
        while (true) {
            val peers = syncMutex.withLock {
                val ready = readyPeers.value - attemptedPeers
                if (ready.isEmpty()) {
                    activeSync = null
                    return
                }
                ready
            }

            for (peer in peers) {
                if (attemptedPeers.add(peer) && peer in readyPeers.value) {
                    try {
                        meshRepo.send(peer = peer, text = "mesh sync")
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        logManager.e(message = "Mesh peer sync failed", error = error)
                    }
                }
            }
        }
    }
}

private const val READY_PEER_TIMEOUT_MS = 30_000L
