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

internal abstract class BluetoothManagerBase : BluetoothManager {

    private val readyPeers: MutableStateFlow<List<MeshPeer>> by lazy {
        MutableStateFlow(emptyList())
    }
    private val readyPeersView: StateFlow<List<MeshPeer>> by lazy { readyPeers.asStateFlow() }
    private val incomingPackets = Channel<BluetoothPacket>(Channel.BUFFERED)
    private val incomingMessages = mutableMapOf<MeshPeer, IncomingMessage>()
    private val sendQueue = Mutex()
    private var nextMessageId = Random.nextInt(0x10000)

    final override fun getReadyPeersFlow(): StateFlow<List<MeshPeer>> = readyPeersView

    final override fun getIncomingPacketsFlow(): Flow<BluetoothPacket> =
        incomingPackets.receiveAsFlow()

    final override fun handleIncomingPacket(peer: MeshPeer, payload: ByteArray): Boolean {
        if (payload.size > MAX_MESH_PAYLOAD_BYTES) return false
        if (payload.firstOrNull() != FRAME_MARKER) {
            incomingMessages.remove(peer)
            return incomingPackets.trySend(BluetoothPacket(peer, payload.copyOf())).isSuccess
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
            var offset = 0
            var index = 0
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
        }
    }

    /** Call this on the same platform callback queue as [handleIncomingPacket]. */
    protected fun updateReadyPeers(peers: List<MeshPeer>) {
        incomingMessages.keys.retainAll(peers)
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
        if (frame.size <= FRAME_HEADER_SIZE) return false
        val messageId = frame.readUnsignedShort(MESSAGE_ID_OFFSET)
        val index = frame.readUnsignedShort(FRAGMENT_INDEX_OFFSET)
        val headerSize = if (index == 0) FIRST_FRAME_HEADER_SIZE else FRAME_HEADER_SIZE
        val message = if (index == 0) {
            incomingMessages.remove(peer)
            if (frame.size <= headerSize) return false
            val size = frame.readUnsignedShort(FRAME_HEADER_SIZE)
            if (size < frame.size - headerSize) return false
            IncomingMessage(messageId, ByteArray(size))
        } else {
            incomingMessages[peer]?.takeIf { it.id == messageId } ?: return false
        }
        val end = message.size + frame.size - headerSize
        if (index != message.nextIndex || end > message.payload.size ||
            (frame.size < MAX_MESH_PAYLOAD_BYTES && end < message.payload.size)
        ) {
            incomingMessages.remove(peer)
            return false
        }
        frame.copyInto(
            destination = message.payload,
            destinationOffset = message.size,
            startIndex = headerSize,
        )
        if (end == message.payload.size) {
            incomingMessages.remove(peer)
            return incomingPackets.trySend(BluetoothPacket(peer, message.payload)).isSuccess
        }
        message.size = end
        message.nextIndex++
        incomingMessages[peer] = message
        return true
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

/** 0xff is invalid in UTF-8, so legacy short text packets need no header. */
private const val FRAME_MARKER: Byte = -1
private const val MESSAGE_ID_OFFSET = 1
private const val FRAGMENT_INDEX_OFFSET = 3
private const val FRAME_HEADER_SIZE = 5
private const val FIRST_FRAME_HEADER_SIZE = 7

private fun ByteArray.readUnsignedShort(offset: Int): Int {
    return ((this[offset].toInt() and 0xff) shl 8) or (this[offset + 1].toInt() and 0xff)
}

private fun ByteArray.writeUnsignedShort(offset: Int, value: Int) {
    this[offset] = (value ushr 8).toByte()
    this[offset + 1] = value.toByte()
}
