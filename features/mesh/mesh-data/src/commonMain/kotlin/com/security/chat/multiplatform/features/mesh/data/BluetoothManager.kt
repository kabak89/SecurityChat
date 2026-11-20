package com.security.chat.multiplatform.features.mesh.data

import com.security.chat.multiplatform.features.mesh.domain.LogManager
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshPeer
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.random.Random

internal interface BluetoothManager {

    fun start()

    fun getReadyPeersFlow(): StateFlow<List<MeshPeer>>

    fun getIncomingPacketsFlow(): Flow<BluetoothPacket>

    /** Returns whether the incoming frame was accepted. */
    fun handleIncomingPacket(peer: MeshPeer, payload: ByteArray): Boolean

    suspend fun send(peer: MeshPeer, payload: ByteArray)
}

internal class BluetoothPacket(val peer: MeshPeer, val payload: ByteArray)

internal expect class BluetoothManagerImpl(logManager: LogManager) : BluetoothManager

internal abstract class BluetoothManagerBase(protected val logManager: LogManager) :
    BluetoothManager {

    private val readyPeers: MutableStateFlow<List<MeshPeer>> by lazy {
        MutableStateFlow(emptyList())
    }
    private val readyPeersView: StateFlow<List<MeshPeer>> by lazy { readyPeers.asStateFlow() }
    private val incomingPackets = Channel<BluetoothPacket>(Channel.BUFFERED)
    private val incomingMessages = mutableMapOf<MeshPeer, IncomingMessage>()
    private val rejectedTransferIds = mutableMapOf<MeshPeer, Int>()
    private val sendQueue = Mutex()
    private var nextMessageId = Random.nextInt(0x10000)

    final override fun getReadyPeersFlow(): StateFlow<List<MeshPeer>> = readyPeersView

    final override fun getIncomingPacketsFlow(): Flow<BluetoothPacket> =
        incomingPackets.receiveAsFlow()

    final override fun handleIncomingPacket(peer: MeshPeer, payload: ByteArray): Boolean {
        if (payload.size > MAX_MESH_PAYLOAD_BYTES) {
            logManager.e("Rejected oversized BLE frame (session=${peer.sessionId}, ${payload.meshFrameDetails()})")
            return false
        }
        if (payload.firstOrNull() != FRAME_MARKER) {
            discardIncomplete(peer = peer, reason = "unfragmented frame arrived")
            return queueReceivedPacket(peer = peer, payload = payload.copyOf(), transferId = null)
        }
        return acceptFragment(peer, payload)
    }

    final override suspend fun send(peer: MeshPeer, payload: ByteArray) {
        require(payload.size <= MAX_MESH_MESSAGE_BYTES) {
            "Mesh message exceeds $MAX_MESH_MESSAGE_BYTES bytes"
        }
        sendQueue.withLock {
            if (payload.size <= MAX_MESH_PAYLOAD_BYTES && payload.firstOrNull() != FRAME_MARKER) {
                sendQueuedFrame(peer, payload)
                return@withLock
            }

            val messageId = nextMessageId
            nextMessageId = (nextMessageId + 1) and 0xffff
            logManager.d {
                "Started BLE transfer (session=${peer.sessionId}, transfer=$messageId, bytes=${payload.size})"
            }
            var offset = 0
            var index = 0
            try {
                while (offset < payload.size) {
                    val headerSize = if (index == 0) FIRST_FRAME_HEADER_SIZE else FRAME_HEADER_SIZE
                    val end = minOf(offset + MAX_MESH_PAYLOAD_BYTES - headerSize, payload.size)
                    val frame = ByteArray(headerSize + end - offset)
                    frame[0] = FRAME_MARKER
                    frame.writeUnsignedShort(MESSAGE_ID_OFFSET, messageId)
                    frame.writeUnsignedShort(FRAGMENT_INDEX_OFFSET, index)
                    if (index == 0) frame.writeUnsignedShort(FRAME_HEADER_SIZE, payload.size)
                    payload.copyInto(
                        destination = frame,
                        destinationOffset = headerSize,
                        startIndex = offset,
                        endIndex = end,
                    )
                    sendQueuedFrame(peer, frame)
                    offset = end
                    index++
                }
            } finally {
                logManager.d {
                    "Finished local BLE transfer attempt (session=${peer.sessionId}, transfer=$messageId, " +
                            "completed=${offset == payload.size}, localFragments=$index, localBytes=$offset/${payload.size})"
                }
            }
        }
    }

    /** Call this on the same platform callback queue as [handleIncomingPacket]. */
    protected fun updateReadyPeers(peers: List<MeshPeer>) {
        for (peer in incomingMessages.keys - peers.toSet()) {
            discardIncomplete(peer = peer, reason = "session ended")
        }
        rejectedTransferIds.keys.retainAll(peers)
        readyPeers.value = peers
    }

    protected abstract suspend fun sendFrame(peer: MeshPeer, payload: ByteArray)

    private suspend fun sendQueuedFrame(peer: MeshPeer, frame: ByteArray) {
        currentCoroutineContext().ensureActive()
        /** Keep the queue locked until the in-flight GATT operation has completed. */
        withContext(NonCancellable) { sendFrame(peer, frame) }
        currentCoroutineContext().ensureActive()
    }

    private fun acceptFragment(peer: MeshPeer, frame: ByteArray): Boolean {
        if (frame.size <= FRAME_HEADER_SIZE) {
            logManager.e("Rejected BLE fragment with a short header (session=${peer.sessionId}, bytes=${frame.size})")
            return false
        }
        val messageId = frame.readUnsignedShort(MESSAGE_ID_OFFSET)
        val index = frame.readUnsignedShort(FRAGMENT_INDEX_OFFSET)
        val headerSize = if (index == 0) FIRST_FRAME_HEADER_SIZE else FRAME_HEADER_SIZE
        val message = if (index == 0) {
            discardIncomplete(peer = peer, reason = "new transfer=$messageId started")
            rejectedTransferIds.remove(peer)
            if (frame.size <= headerSize) {
                logManager.e("Rejected first BLE fragment without data (session=${peer.sessionId}, ${frame.meshFrameDetails()})")
                return false
            }
            val size = frame.readUnsignedShort(FRAME_HEADER_SIZE)
            if (size < frame.size - headerSize) {
                logManager.e(
                    "Rejected first BLE fragment with invalid size (session=${peer.sessionId}, " +
                            "${frame.meshFrameDetails()}, declaredBytes=$size)",
                )
                return false
            }
            logManager.d { "Started receiving BLE transfer (session=${peer.sessionId}, transfer=$messageId, bytes=$size)" }
            IncomingMessage(messageId, ByteArray(size))
        } else {
            val active = incomingMessages[peer]
            if (active?.id != messageId) {
                if (rejectedTransferIds.put(peer, messageId) != messageId) {
                    logManager.e(
                        "Rejected BLE fragment without matching first fragment (session=${peer.sessionId}, " +
                                "${frame.meshFrameDetails()}, activeTransfer=${active?.id})",
                    )
                }
                return false
            }
            active
        }
        val end = message.size + frame.size - headerSize
        if (index != message.nextIndex || end > message.payload.size ||
            (frame.size < MAX_MESH_PAYLOAD_BYTES && end < message.payload.size)
        ) {
            incomingMessages.remove(peer)
            rejectedTransferIds[peer] = messageId
            logManager.e(
                "Rejected BLE fragment during assembly (session=${peer.sessionId}, ${frame.meshFrameDetails()}, " +
                        "expectedFragment=${message.nextIndex}, assembledBytes=${message.size}/${message.payload.size})",
            )
            return false
        }
        frame.copyInto(
            destination = message.payload,
            destinationOffset = message.size,
            startIndex = headerSize,
        )
        if (end == message.payload.size) {
            incomingMessages.remove(peer)
            logManager.d {
                "Assembled BLE transfer (session=${peer.sessionId}, transfer=$messageId, fragments=${index + 1}, bytes=$end)"
            }
            return queueReceivedPacket(
                peer = peer,
                payload = message.payload,
                transferId = messageId,
            )
        }
        message.size = end
        message.nextIndex++
        if (message.nextIndex % RECEIVE_PROGRESS_INTERVAL == 0) {
            logManager.d {
                "Receiving BLE transfer (session=${peer.sessionId}, transfer=$messageId, " +
                        "nextFragment=${message.nextIndex}, assembledBytes=$end/${message.payload.size})"
            }
        }
        incomingMessages[peer] = message
        return true
    }

    private fun discardIncomplete(peer: MeshPeer, reason: String) {
        val message = incomingMessages.remove(peer) ?: return
        logManager.e(
            "Discarded incomplete BLE transfer (session=${peer.sessionId}, transfer=${message.id}, " +
                    "nextFragment=${message.nextIndex}, assembledBytes=${message.size}/${message.payload.size}, reason=$reason)",
        )
    }

    private fun queueReceivedPacket(peer: MeshPeer, payload: ByteArray, transferId: Int?): Boolean {
        val result = incomingPackets.trySend(BluetoothPacket(peer, payload))
        if (!result.isSuccess) {
            logManager.e(
                "Could not queue received BLE packet (session=${peer.sessionId}, transfer=$transferId, " +
                        "bytes=${payload.size}, queueClosed=${result.isClosed})",
            )
        }
        return result.isSuccess
    }

    private class IncomingMessage(val id: Int, val payload: ByteArray) {

        var size = 0
        var nextIndex = 0
    }
}

internal const val MESH_SERVICE_UUID = "DDC1B612-6B19-4D08-AD8A-0C7036F6CD80"
internal const val MESH_WRITE_UUID = "DDC1B612-6B19-4D08-AD8A-0C7036F6CD81"
internal const val MESH_NOTIFY_UUID = "DDC1B612-6B19-4D08-AD8A-0C7036F6CD82"
internal const val MAX_MESH_PAYLOAD_BYTES = 20
internal const val MAX_MESH_MESSAGE_BYTES = 0xffff

internal fun ByteArray.meshFrameDetails(): String {
    if (size < FRAME_HEADER_SIZE || first() != FRAME_MARKER) return "bytes=$size"
    return "transfer=${readUnsignedShort(MESSAGE_ID_OFFSET)}, " +
            "fragment=${readUnsignedShort(FRAGMENT_INDEX_OFFSET)}, bytes=$size"
}

internal fun ByteArray.isFirstMeshFragment(): Boolean {
    return size > FRAME_HEADER_SIZE && first() == FRAME_MARKER && readUnsignedShort(
        FRAGMENT_INDEX_OFFSET,
    ) == 0
}

/** 0xff is invalid in UTF-8, so legacy short text packets need no header. */
private const val FRAME_MARKER: Byte = -1
private const val MESSAGE_ID_OFFSET = 1
private const val FRAGMENT_INDEX_OFFSET = 3
private const val FRAME_HEADER_SIZE = 5
private const val FIRST_FRAME_HEADER_SIZE = 7
private const val RECEIVE_PROGRESS_INTERVAL = 32

private fun ByteArray.readUnsignedShort(offset: Int): Int {
    return ((this[offset].toInt() and 0xff) shl 8) or (this[offset + 1].toInt() and 0xff)
}

private fun ByteArray.writeUnsignedShort(offset: Int, value: Int) {
    this[offset] = (value ushr 8).toByte()
    this[offset + 1] = value.toByte()
}
