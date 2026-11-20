package com.security.chat.multiplatform.features.mesh.data

import com.security.chat.multiplatform.common.background.work.BackgroundWorkConditions
import com.security.chat.multiplatform.common.background.work.BackgroundWorkManager
import com.security.chat.multiplatform.common.background.work.BackgroundWorkManager.Type
import com.security.chat.multiplatform.common.background.work.BackgroundWorkPolicy
import com.security.chat.multiplatform.common.background.work.BackgroundWorkPriority
import com.security.chat.multiplatform.common.background.work.BackgroundWorkSchedule
import com.security.chat.multiplatform.common.core.error.AppError
import com.security.chat.multiplatform.common.core.error.NetworkError
import com.security.chat.multiplatform.common.core.network.ConnectivityObserver
import com.security.chat.multiplatform.common.core.time.TimeProvider
import com.security.chat.multiplatform.common.encryption.verifyRsa
import com.security.chat.multiplatform.common.settings.PublicSettings
import com.security.chat.multiplatform.features.chat.data.common.ChatEventsPublisher
import com.security.chat.multiplatform.features.chat.data.common.OutgoingMessageScheduler
import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageRecipientSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.Status
import com.security.chat.multiplatform.features.chats.data.common.ChatsDataHelper
import com.security.chat.multiplatform.features.chats.data.storage.ChatsStorage
import com.security.chat.multiplatform.features.mesh.data.storage.MeshMessageStorage
import com.security.chat.multiplatform.features.mesh.data.storage.MeshPacketRow
import com.security.chat.multiplatform.features.mesh.domain.LogManager
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshIncomingPacket
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshPeer
import com.security.chat.multiplatform.features.mesh.domain.repo.MeshRepo
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import com.security.chat.multiplatform.features.users.data.common.UsersDataHelper
import com.security.chat.multiplatform.features.users.data.storage.UsersStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

public class MeshMessageService internal constructor(
    private val repo: MeshRepo,
    private val storage: MeshMessageStorage,
    private val chatStorage: ChatStorage,
    private val chatsStorage: ChatsStorage,
    private val chats: ChatsDataHelper,
    private val usersStorage: UsersStorage,
    private val users: UsersDataHelper,
    private val userStorage: UserStorage,
    private val outgoing: OutgoingMessageScheduler,
    private val chatEvents: ChatEventsPublisher,
    private val notifier: MeshIncomingNotifier,
    private val settings: PublicSettings,
    private val backgroundWork: BackgroundWorkManager,
    private val connectivity: ConnectivityObserver,
    private val crypto: MeshMessageCrypto,
    private val api: MeshRelayApi,
    private val timeProvider: TimeProvider,
    private val json: Json,
    private val log: LogManager,
) {

    private val stateLock = Mutex()
    private val exchangeLock = Mutex()
    private val uploadLock = Mutex()
    private val schedulingLock = Mutex()
    private val controlFrameLock = Mutex()
    private val peerHellos = mutableMapOf<MeshPeer, MeshFrame.Hello>()
    private val sentHellos = mutableSetOf<MeshPeer>()
    private val sentFrames = mutableMapOf<ConnectedPeer, MutableSet<String>>()
    private val pendingReceipts = mutableMapOf<String, SignedMeshData>()
    private val pendingAcknowledgements = Channel<PendingPacketAcknowledgement>(Channel.BUFFERED)
    private val outgoingRevision = MutableStateFlow(0L)
    private val exchangeRequests = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private var meshDeviceId: String? = null

    public fun start(scope: CoroutineScope) {
        log.d { "Mesh message service started" }
        exchangeRequests.onEach {
            attempt("scheduling BLE exchange") {
                schedulingLock.withLock {
                    val input = currentInput() ?: return@withLock
                    enqueue(online = false, input = input)
                }
            }
        }.launchIn(scope)
        repo.getIncomingPacketsFlow().onEach { packet ->
            attempt("processing BLE frame from session=${packet.peer.sessionId}") { handle(packet) }
        }.launchIn(scope)
        pendingAcknowledgements.receiveAsFlow().onEach { acknowledgement ->
            attempt("acknowledging a received BLE packet") { sendAcknowledgement(acknowledgement) }
        }.launchIn(scope)
        repo.getReadyPeersFlow().onEach { peers ->
            stateLock.withLock {
                peerHellos.keys.retainAll(peers.toSet())
            }
            log.d { "Ready BLE peers changed (count=${peers.size}, sessions=${peers.joinToString { it.sessionId }})" }
            if (peers.isNotEmpty()) {
                scheduleExchange()
                if (connectivity.isOnline.value && settings.getString(RECEIPT_KEY) == null) scheduleUpload()
            }
        }.launchIn(scope)
        connectivity.isOnline.onEach { online ->
            log.d { "Mesh connectivity changed (online=$online)" }
            scheduleUpload()
            scheduleExchange()
        }.launchIn(scope)
        userStorage.sessionUserId.onEach { userId ->
            log.d { "Mesh account session changed (signedIn=${userId != null})" }
            if (userId != null) {
                scheduleExchangeRetry()
                scheduleUpload()
                scheduleExchange()
            }
        }.launchIn(scope)
        outgoing.savedMessages.onEach {
            outgoingRevision.update { it + 1 }
            log.d { "Outgoing chat queue updated, requesting mesh work" }
            scheduleExchange()
        }.launchIn(scope)
        outgoingRevision.filter { it != 0L }.onEach { scheduleUpload() }.launchIn(scope)
    }

    public fun scheduleExchange() {
        exchangeRequests.tryEmit(Unit)
    }

    public suspend fun clear() {
        log.d { "Cancelling mesh work for sign-out" }
        schedulingLock.withLock {
            for (key in listOf(
                MESH_EXCHANGE_WORK,
                MESH_EXCHANGE_RETRY,
                MESH_UPLOAD_WORK,
                MESH_UPLOAD_RETRY,
            )) {
                attempt("cancelling mesh work") { backgroundWork.cancel(key) }
            }
        }
        uploadLock.withLock {
            exchangeLock.withLock {
                controlFrameLock.withLock {
                    stateLock.withLock {
                        storage.clear()
                        settings.putString(DEVICE_ID_KEY, null)
                        meshDeviceId = null
                        sentHellos.clear()
                        sentFrames.clear()
                        pendingReceipts.clear()
                        log.d { "Cleared the mesh device ID, message queue and receipt history for sign-out" }
                    }
                }
            }
        }
    }

    internal suspend fun runWork(
        key: String,
        input: Map<String, String>,
        work: suspend (ownerId: String, deviceId: String) -> Unit,
    ) {
        val ownerId = input[MESH_USER_ID]
        val deviceId = input[MESH_AUTH_DEVICE_ID]
        if (ownerId == null || deviceId == null) {
            log.e(message = "Mesh worker has no account session input (key=$key)")
            return
        }
        log.d { "Mesh worker started (key=$key)" }
        try {
            work(ownerId, deviceId)
        } catch (error: CancellationException) {
            log.d { "Mesh worker cancelled (key=$key)" }
            throw error
        } catch (error: Exception) {
            log.e(message = "Mesh worker failed (key=$key, error=${error::class.simpleName})")
            throw error
        }
    }

    internal suspend fun exchange(ownerId: String, originDeviceId: String, retry: Boolean) {
        exchangeLock.withLock {
            val peers = repo.getReadyPeersFlow().value
            val readyPeers = peers.toSet()
            sentFrames.keys.removeAll { it.peer !in readyPeers }
            if (!isCurrentSession(ownerId, originDeviceId)) {
                log.d { "BLE exchange skipped because the account session changed" }
                return
            }
            if (retry) {
                sentFrames.values.forEach { sent -> sent.retainAll { it.startsWith("receipt:") } }
            }
            controlFrameLock.withLock {
                if (retry) sentHellos.clear() else sentHellos.retainAll(readyPeers)
            }
            val now = now()
            val packets = stateLock.withLock {
                removeExpiredPackets(now)
                if (peers.isEmpty()) emptyList() else storage.getPackets(ownerId, now)
            }
            if (peers.isEmpty()) {
                log.d { "BLE exchange skipped, waiting for a ready peer" }
                return
            }
            val localDeviceId = stateLock.withLock {
                if (!isCurrentSession(ownerId, originDeviceId)) return
                getOrCreateMeshDeviceId()
            }
            val initialRevision = outgoingRevision.value
            val seenMessageIds = mutableSetOf<String>()
            val outgoingPackets = prepareOutgoing(
                ownerId = ownerId,
                now = now,
                seenMessageIds = seenMessageIds,
            )
            val relayPackets =
                packets.filter { it.outgoing == 0L && it.packet != null && it.receipt == null }
            log.d {
                "BLE exchange ready (peers=${peers.size}, online=${connectivity.isOnline.value}, retry=$retry, " +
                        "outgoing=${outgoingPackets.size}, relay=${relayPackets.size}, " +
                        "receipts=${packets.count { it.receipt != null }})"
            }

            val connectedPeers = mutableListOf<ConnectedPeer>()
            val pendingTransfers = outgoingPackets + relayPackets
            val peerBacklogs = mutableListOf<Sequence<ExchangeTransfer>>()
            for (peer in peers) {
                if (!isCurrentSession(ownerId, originDeviceId)) return
                attempt("exchanging data with BLE session=${peer.sessionId}") {
                    controlFrameLock.withLock { sendHello(peer, localDeviceId) }
                    val peerId = stateLock.withLock { peerHellos[peer]?.deviceId } ?: run {
                        log.d { "Waiting for mesh hello (session=${peer.sessionId})" }
                        return@attempt
                    }
                    if (peerId == localDeviceId) return@attempt
                    val target = ConnectedPeer(peer, peerId)
                    sentFrames.keys.removeAll { it.peer == peer && it != target }
                    val receipts = prepareReceiptBatches(target, packets)
                    connectedPeers += target
                    peerBacklogs += receipts.asSequence() +
                            pendingTransfers.asSequence()
                                .map { ExchangeTransfer.Message(target, it) }
                }
            }
            val skippedPreviouslySent = sendTransfers(
                ownerId = ownerId,
                originDeviceId = originDeviceId,
                peers = connectedPeers,
                backlog = peerBacklogs.asSequence().flatten().iterator(),
                initialRevision = initialRevision,
                seenMessageIds = seenMessageIds,
            )
            log.d { "BLE exchange finished (skippedPreviouslySent=$skippedPreviouslySent)" }
        }
    }

    internal suspend fun upload(ownerId: String, originDeviceId: String) {
        uploadLock.withLock {
            val currentSession = isCurrentSession(ownerId, originDeviceId)
            val online = connectivity.isOnline.value
            if (!currentSession || !online) {
                log.d { "Mesh upload skipped (online=$online, currentSession=$currentSession)" }
                return
            }
            val key = getReceiptKey() ?: return
            val packets = stateLock.withLock {
                removeExpiredPackets(now())
                if (pendingReceipts.isNotEmpty()) {
                    log.d { "Checking deferred mesh receipts (count=${pendingReceipts.size})" }
                }
                for (receipt in pendingReceipts.values) {
                    attempt("checking a deferred BLE receipt") {
                        acceptReceipt(ownerId = ownerId, receipt = receipt, key = key)
                    }
                }
                pendingReceipts.clear()
                storage.getPackets(ownerId, now())
            }
            log.d {
                val pending =
                    packets.count { it.outgoing == 0L && it.packet != null && it.receipt == null }
                "Mesh upload queue checked (pending=$pending, pendingLocal=${packets.count { it.pendingDisplay != 0L }})"
            }
            for (row in packets) {
                val sameSession = isCurrentSession(ownerId, originDeviceId)
                val stillOnline = connectivity.isOnline.value
                if (!sameSession || !stillOnline) {
                    log.d { "Mesh upload paused (online=$stillOnline, currentSession=$sameSession)" }
                    return
                }
                if (row.isExpired(now())) continue
                if (row.pendingDisplay != 0L && row.packet != null) {
                    attempt("displaying BLE message=${row.messageId} after reconnect") {
                        val signed = json.decodeFromString<SignedMeshData>(checkNotNull(row.packet))
                        val payload = json.decodeFromString<MeshMessagePayload>(signed.payload)
                        if (chatsStorage.getChat(payload.message.chatId) == null) {
                            chats.fetchChatInfo(payload.message.chatId)
                        }
                        if (usersStorage.getUser(payload.authorId) == null) {
                            users.getOrFetchUser(payload.authorId)
                        }
                        stateLock.withLock {
                            if (isCurrentSession(ownerId, originDeviceId)) {
                                storage.get(ownerId, row.packetHash)?.let { display(it) }
                            }
                        }
                    }
                }
                if (row.outgoing != 0L) continue
                attempt("uploading mesh message=${row.messageId}") {
                    uploadPacket(row, originDeviceId, key)
                }
            }
            scheduleExchange()
            log.d { "Mesh upload pass finished" }
        }
    }

    private suspend fun uploadPacket(row: MeshPacketRow, originDeviceId: String, key: String) {
        val current = stateLock.withLock { storage.get(row.ownerId, row.packetHash) } ?: return
        if (current.receipt != null || current.packet == null || current.isExpired(now())) return
        val packet = json.decodeFromString<SignedMeshData>(checkNotNull(current.packet))
        log.d {
            "Uploading mesh packet to server (message=${row.messageId}, packet=${
                row.packetHash.take(
                    12,
                )
            })"
        }
        val receipt = try {
            api.upload(packet)
        } catch (error: NetworkError) {
            if (error.statusCode !in setOf(400, 403, 410, 413, 422)) {
                log.d { "Mesh message=${row.messageId} remains queued after HTTP ${error.statusCode}" }
                throw error
            }
            stateLock.withLock { storage.discard(row.ownerId, row.packetHash) }
            log.e(message = "Server rejected mesh packet (message=${row.messageId}, HTTP ${error.statusCode}), discarded")
            return
        }
        log.d { "Received a mesh acceptance receipt from server (message=${row.messageId})" }
        stateLock.withLock {
            if (isCurrentSession(row.ownerId, originDeviceId)) {
                acceptReceipt(ownerId = row.ownerId, receipt = receipt, key = key)
            }
        }
    }

    private suspend fun handle(incoming: MeshIncomingPacket) {
        if (!incoming.text.startsWith('{') || incoming.text.encodeToByteArray().size > MAX_PACKET_BYTES) {
            log.e(message = "Rejected BLE frame with invalid format or size (session=${incoming.peer.sessionId})")
            return
        }
        val frame = json.decodeFromString<MeshFrame>(incoming.text)
        val ownerId = userStorage.getUserId()
        val originDeviceId = userStorage.getDeviceId()
        if (ownerId == null || originDeviceId == null) {
            log.d { "Ignored BLE frame without an active account session (session=${incoming.peer.sessionId})" }
            return
        }
        var uploadNeeded = false
        val exchangeNeeded = stateLock.withLock {
            if (!isCurrentSession(ownerId, originDeviceId)) return
            when (frame) {
                is MeshFrame.Hello -> {
                    if (!isUuid(frame.deviceId)) {
                        log.e(message = "Rejected mesh hello with invalid device ID (session=${incoming.peer.sessionId})")
                        return
                    }
                    if (peerHellos[incoming.peer] == frame) return
                    peerHellos[incoming.peer] = frame
                    log.d {
                        "Received mesh hello (session=${incoming.peer.sessionId}, peer=${
                            frame.deviceId.take(
                                8,
                            )
                        })"
                    }
                    true
                }

                is MeshFrame.Message -> {
                    val hello = peerHellos[incoming.peer] ?: run {
                        log.d { "Ignored mesh message before hello (session=${incoming.peer.sessionId})" }
                        return
                    }
                    val localDeviceId = getOrCreateMeshDeviceId()
                    if (hello.deviceId == localDeviceId) return
                    val received =
                        receive(ownerId = ownerId, peerId = hello.deviceId, packet = frame.packet)
                            ?: return
                    if (hello.supportsPacketAcknowledgements) {
                        val acknowledgement = PendingPacketAcknowledgement(
                            ownerId = ownerId,
                            originDeviceId = originDeviceId,
                            target = ConnectedPeer(incoming.peer, hello.deviceId),
                            localDeviceId = localDeviceId,
                            packetHash = received.packetHash,
                        )
                        if (pendingAcknowledgements.trySend(acknowledgement).isFailure) {
                            log.e(message = "Could not queue BLE packet acknowledgement (session=${incoming.peer.sessionId})")
                        }
                    }
                    uploadNeeded = received.queueChanged
                    received.queueChanged
                }

                is MeshFrame.PacketAcknowledgement -> {
                    val hello = peerHellos[incoming.peer]
                    if (hello?.supportsPacketAcknowledgements != true || hello.deviceId == getOrCreateMeshDeviceId() ||
                        !PACKET_HASH_PATTERN.matches(frame.packetHash)
                    ) {
                        log.d { "Ignored BLE packet acknowledgement (session=${incoming.peer.sessionId})" }
                        return
                    }
                    val stored = storage.rememberTransfer(
                        ownerId = ownerId,
                        peerId = hello.deviceId,
                        packetHash = frame.packetHash,
                        now = now(),
                    )
                    if (stored) {
                        log.d {
                            "Recorded BLE packet acknowledgement (peer=${hello.deviceId.take(8)}, " +
                                    "packet=${frame.packetHash.take(12)})"
                        }
                    }
                    false
                }

                is MeshFrame.Receipts -> {
                    if (incoming.peer !in peerHellos || frame.receipts.size > RECEIPTS_PER_FRAME) {
                        log.e(
                            message = "Rejected mesh receipt batch before hello or above the receipt limit " +
                                    "(session=${incoming.peer.sessionId})",
                        )
                        return
                    }
                    log.d { "Received mesh receipt batch (session=${incoming.peer.sessionId}, count=${frame.receipts.size})" }
                    val key = settings.getString(RECEIPT_KEY)
                    if (key == null) {
                        for (receipt in frame.receipts) {
                            val payload = json.decodeFromString<MeshReceiptPayload>(receipt.payload)
                            if (pendingReceipts.size < MAX_QUEUED_RELAY_PACKETS &&
                                storage.get(ownerId, payload.packetHash)?.packet != null
                            ) {
                                pendingReceipts[payload.packetHash] = receipt
                            }
                        }
                        log.d {
                            "Deferred mesh receipt verification until the server key is available " +
                                    "(pending=${pendingReceipts.size})"
                        }
                        uploadNeeded = true
                        false
                    } else {
                        frame.receipts.map { receipt ->
                            attempt("checking a BLE receipt") {
                                acceptReceipt(ownerId = ownerId, receipt = receipt, key = key)
                            } == true
                        }.any { it }
                    }
                }
            }
        }
        if (uploadNeeded) scheduleUpload()
        if (exchangeNeeded) scheduleExchange()
    }

    private suspend fun receive(
        ownerId: String,
        peerId: String,
        packet: SignedMeshData,
    ): ReceivedPacket? {
        val payload = json.decodeFromString<MeshMessagePayload>(packet.payload)
        val now = now()
        if (!payload.isValid(now)) {
            val reason = if (payload.expiresAt <= now) "expired" else "invalid metadata"
            log.e(message = "Rejected mesh message packet ($reason, peer=${peerId.take(8)})")
            return null
        }
        val hash = packet.packetHash()
        val existing = storage.get(ownerId, hash)
        if (existing != null) {
            if (existing.isExpired(now)) {
                log.d { "Ignored mesh packet past its local relay deadline (message=${existing.messageId})" }
                return ReceivedPacket(packetHash = hash, queueChanged = false)
            }
            if (peerId in storage.getSources(hash)) {
                log.d {
                    "Ignored duplicate mesh packet (message=${existing.messageId}, peer=${
                        peerId.take(
                            8,
                        )
                    })"
                }
                return ReceivedPacket(packetHash = hash, queueChanged = false)
            }
            storage.save(existing, peerId)
            log.d {
                "Recorded another source for mesh packet (message=${existing.messageId}, " +
                        "peer=${peerId.take(8)}, hasReceipt=${existing.receipt != null})"
            }
            return ReceivedPacket(packetHash = hash, queueChanged = existing.receipt != null)
        }
        val publicKey = usersStorage.getUser(payload.authorId)?.publicKey
        if (publicKey != null && !crypto.isAuthentic(packet, publicKey)) {
            log.e(message = "Rejected mesh packet with invalid author signature (message=${payload.message.id})")
            return null
        }
        removeExpiredPackets(now)
        if (storage.countRelayPackets(ownerId, now) >= MAX_QUEUED_RELAY_PACKETS) {
            log.e(message = "Rejected mesh packet because the relay queue is full (message=${payload.message.id})")
            return null
        }
        val row = MeshPacketRow(
            packetHash = hash,
            ownerId = ownerId,
            messageId = payload.message.id,
            expiresAt = payload.expiresAt,
            outgoing = 0,
            packet = json.encodeToString(packet),
            receipt = null,
            pendingDisplay = if (payload.authorId != ownerId &&
                payload.message.recipients.any { it.recipientId == ownerId }
            ) {
                1
            } else {
                0
            },
            relayExpiresAt = now + RELAY_RETENTION,
        )
        storage.save(row, peerId)
        log.d {
            "Queued received mesh packet (message=${row.messageId}, packet=${hash.take(12)}, " +
                    "peer=${peerId.take(8)}, localRecipient=${row.pendingDisplay != 0L}, authorVerified=${publicKey != null})"
        }
        attempt("displaying received mesh message=${row.messageId}") { display(row) }
        return ReceivedPacket(packetHash = hash, queueChanged = true)
    }

    private suspend fun display(row: MeshPacketRow) {
        if (row.pendingDisplay == 0L || row.packet == null || row.isExpired(now())) return
        val packet = json.decodeFromString<SignedMeshData>(checkNotNull(row.packet))
        val payload = json.decodeFromString<MeshMessagePayload>(packet.payload)
        val message = payload.message
        val chat = chatsStorage.getChat(message.chatId) ?: run {
            log.d { "Mesh message=${message.id} awaits chat metadata before local display" }
            return
        }
        val members = chat.members + chat.authorId
        if (payload.authorId !in members || row.ownerId !in members) {
            log.d { "Deferred display of mesh message=${message.id}, cached chat membership does not match" }
            return
        }
        val publicKey = usersStorage.getUser(payload.authorId)?.publicKey ?: run {
            log.d { "Mesh message=${message.id} awaits the author's public key before local display" }
            return
        }
        if (!crypto.isAuthentic(packet, publicKey)) {
            storage.discard(row.ownerId, row.packetHash)
            log.e(message = "Discarded mesh packet with invalid author signature (message=${message.id})")
            return
        }
        val text = crypto.decrypt(payload, row.ownerId)
        val inserted = chatStorage.saveNewIncomingMessage(
            message = MessageSM.Text(
                id = message.id,
                chatId = message.chatId,
                recipients = listOf(MessageRecipientSM(row.ownerId, Status.ReceivedByReceiver)),
                authorId = payload.authorId,
                timestamp = message.timestamp,
                text = text,
                replyToMessageId = message.replyToMessageId,
            ),
            receivedByUserId = row.ownerId,
        )
        storage.finishDisplay(row.ownerId, row.packetHash)
        if (inserted) {
            log.d { "Saved BLE message=${message.id} to the local chat and queued recipient confirmation" }
            chatEvents.start(message.chatId)
            log.d { "Requesting local chat notification for BLE message=${message.id}" }
            notifier.notify(message.chatId)
        } else {
            log.d { "Local chat skipped inserting mesh message=${message.id}, already stored or deleted" }
        }
    }

    private suspend fun prepareOutgoing(
        ownerId: String,
        now: Long,
        seenMessageIds: MutableSet<String>,
    ): List<MeshPacketRow> {
        return outgoing.sendDeleteMutex.withLock {
            if (userStorage.getUserId() != ownerId) return@withLock emptyList()
            chatStorage.migrateMessageStatuses(ownerId)
            val texts = chatStorage.getOutgoingTextForMesh(ownerId)
            log.d { "Checked outgoing text messages for BLE (count=${texts.size})" }
            texts.filter { seenMessageIds.add(it.id) }.mapNotNull { message ->
                val stored = storage.getOutgoing(ownerId, message.id)
                if (stored == null) {
                    return@mapNotNull packOutgoing(
                        ownerId = ownerId,
                        message = message,
                        createdAt = now,
                    )
                }
                if (stored.expiresAt <= now || (stored.packet == null && stored.receipt == null)) {
                    return@mapNotNull null
                }
                stored.packet?.let { rawPacket ->
                    val packet = json.decodeFromString<SignedMeshData>(rawPacket)
                    val payload = json.decodeFromString<MeshMessagePayload>(packet.payload)
                    if (message.hasSameMeshRecipients(payload)) return@mapNotNull stored
                }
                packOutgoing(
                    ownerId = ownerId,
                    message = message,
                    createdAt = stored.expiresAt - PACKET_DURATION,
                )
            }
        }
    }

    private suspend fun packOutgoing(
        ownerId: String,
        message: MessageSM.Text,
        createdAt: Long,
    ): MeshPacketRow? {
        return attempt("packing outgoing mesh message=${message.id}") {
            val packet = crypto.pack(message, createdAt) ?: return@attempt null
            if (json.encodeToString<MeshFrame>(MeshFrame.Message(packet)).encodeToByteArray().size >
                MAX_PACKET_BYTES
            ) {
                log.d { "Mesh message=${message.id} exceeds the BLE packet size limit" }
                return@attempt null
            }
            val row = MeshPacketRow(
                packetHash = packet.packetHash(),
                ownerId = ownerId,
                messageId = message.id,
                expiresAt = createdAt + PACKET_DURATION,
                outgoing = 1,
                packet = json.encodeToString(packet),
                receipt = null,
                pendingDisplay = 0,
                relayExpiresAt = null,
            )
            storage.save(row, sourceId = null)
            log.d {
                "Prepared outgoing mesh packet (message=${row.messageId}, packet=${
                    row.packetHash.take(
                        12,
                    )
                })"
            }
            row
        }
    }

    private suspend fun prepareReceiptBatches(
        target: ConnectedPeer,
        packets: List<MeshPacketRow>,
    ): List<ExchangeTransfer.Receipts> {
        val sent = sentFrames.getOrPut(target) { mutableSetOf() }
        val eligible = packets.filter { row ->
            row.receipt != null && "receipt:${row.packetHash}" !in sent &&
                    target.deviceId in storage.getSources(row.packetHash)
        }
        return eligible.chunked(RECEIPTS_PER_FRAME).map { ExchangeTransfer.Receipts(target, it) }
    }

    private suspend fun sendReceiptBatch(transfer: ExchangeTransfer.Receipts) {
        val (peer, peerId) = transfer.target
        val batch = transfer.rows.filter { it.expiresAt > now() }
        if (batch.isEmpty()) return
        val receipts = batch.map { json.decodeFromString<SignedMeshData>(checkNotNull(it.receipt)) }
        log.d {
            "Returning mesh receipts over BLE (session=${peer.sessionId}, peer=${peerId.take(8)}, count=${batch.size})"
        }
        repo.send(peer = peer, text = json.encodeToString<MeshFrame>(MeshFrame.Receipts(receipts)))
        val sent = sentFrames.getOrPut(transfer.target) { mutableSetOf() }
        batch.forEach { sent.add("receipt:${it.packetHash}") }
        log.d {
            "Returned mesh receipts over BLE (session=${peer.sessionId}, peer=${peerId.take(8)}, " +
                    "messages=${batch.joinToString { it.messageId }})"
        }
    }

    private suspend fun sendTransfers(
        ownerId: String,
        originDeviceId: String,
        peers: List<ConnectedPeer>,
        backlog: Iterator<ExchangeTransfer>,
        initialRevision: Long,
        seenMessageIds: MutableSet<String>,
    ): Int {
        var skippedPreviouslySent = 0
        var revision = initialRevision
        var preferFresh = true
        val fresh = ArrayDeque<ExchangeTransfer.Message>()
        val failedPeers = mutableSetOf<ConnectedPeer>()
        while (failedPeers.size < peers.size) {
            if (!isCurrentSession(ownerId, originDeviceId)) {
                log.d { "Paused sending mesh messages over BLE because the account session changed" }
                break
            }
            val currentRevision = outgoingRevision.value
            if (revision != currentRevision) {
                revision = currentRevision
                val outgoingPackets = prepareOutgoing(
                    ownerId = ownerId,
                    now = now(),
                    seenMessageIds = seenMessageIds,
                )
                for (row in outgoingPackets) {
                    for (peer in peers) {
                        if (peer !in failedPeers) fresh.addLast(ExchangeTransfer.Message(peer, row))
                    }
                }
                log.d { "Refreshed outgoing BLE messages during exchange (new=${outgoingPackets.size})" }
            }
            val useFresh = fresh.isNotEmpty() && (preferFresh || !backlog.hasNext())
            val transfer = when {
                useFresh -> fresh.removeFirst()
                backlog.hasNext() -> backlog.next()
                else -> break
            }
            if (transfer.target in failedPeers) continue
            val peerStillCurrent = stateLock.withLock {
                peerHellos[transfer.target.peer]?.deviceId == transfer.target.deviceId
            }
            if (!peerStillCurrent) {
                failedPeers += transfer.target
                continue
            }
            preferFresh = !useFresh
            val skipped =
                attempt("exchanging data with BLE session=${transfer.target.peer.sessionId}") {
                    when (transfer) {
                        is ExchangeTransfer.Message -> sendPacket(transfer, originDeviceId)
                        is ExchangeTransfer.Receipts -> {
                            sendReceiptBatch(transfer)
                            false
                        }
                    }
                }
            if (skipped == null) failedPeers += transfer.target
            if (skipped == true) skippedPreviouslySent++
        }
        return skippedPreviouslySent
    }

    private suspend fun sendPacket(
        transfer: ExchangeTransfer.Message,
        originDeviceId: String,
    ): Boolean {
        val peerId = transfer.target.deviceId
        val row = transfer.row
        if (row.isExpired(now())) return false
        if (storage.hasTransfer(peerId, row.packetHash)) return true
        if (row.outgoing == 0L) {
            val current = storage.get(row.ownerId, row.packetHash) ?: return false
            val rawPacket = current.packet ?: return false
            if (current.receipt != null || peerId in storage.getSources(row.packetHash)) return false
            val frame = MeshFrame.Message(json.decodeFromString<SignedMeshData>(rawPacket))
            if (current.isExpired(now())) return false
            return !sendPacketOnce(transfer.target, row, frame)
        }
        val packet = json.decodeFromString<SignedMeshData>(row.packet ?: return false)
        val payload = json.decodeFromString<MeshMessagePayload>(packet.payload)
        val message = payload.message
        return outgoing.sendDeleteMutex.withLock {
            if (!isCurrentSession(row.ownerId, originDeviceId)) return@withLock false
            val stored = chatStorage.getMessage(chatId = message.chatId, messageId = message.id)
            if (stored == null || stored.authorId != row.ownerId || !stored.hasSameMeshRecipients(
                    payload,
                ) ||
                stored.recipients.none {
                    it.status == Status.Created || (it.userId != row.ownerId && it.status == Status.Sent)
                } ||
                chatStorage.isMessageDeletionPending(
                    chatId = message.chatId,
                    messageId = message.id,
                    authorId = row.ownerId,
                )
            ) {
                log.d {
                    "Skipped outgoing BLE message=${row.messageId}, no longer pending, " +
                            "awaiting deletion, or recipients changed"
                }
                return@withLock false
            }
            val frame = MeshFrame.Message(packet)
            !sendPacketOnce(transfer.target, row, frame)
        }
    }

    private suspend fun sendHello(peer: MeshPeer, localDeviceId: String) {
        if (peer in sentHellos) return
        val frame = MeshFrame.Hello(localDeviceId, supportsPacketAcknowledgements = true)
        repo.send(peer = peer, text = json.encodeToString<MeshFrame>(frame))
        sentHellos.add(peer)
        log.d { "Sent mesh hello (session=${peer.sessionId})" }
    }

    private suspend fun sendPacketOnce(
        target: ConnectedPeer,
        row: MeshPacketRow,
        frame: MeshFrame.Message,
    ): Boolean {
        val sent = sentFrames.getOrPut(target) { mutableSetOf() }
        if (row.packetHash in sent) return false
        log.d {
            "Sending mesh packet over BLE (message=${row.messageId}, packet=${row.packetHash.take(12)}, " +
                    "session=${target.peer.sessionId})"
        }
        repo.send(peer = target.peer, text = json.encodeToString<MeshFrame>(frame))
        sent.add(row.packetHash)
        log.d {
            "Sent mesh packet over BLE (message=${row.messageId}, packet=${row.packetHash.take(12)}, " +
                    "session=${target.peer.sessionId})"
        }
        return true
    }

    private suspend fun acceptReceipt(
        ownerId: String,
        receipt: SignedMeshData,
        key: String,
    ): Boolean {
        val payload = json.decodeFromString<MeshReceiptPayload>(receipt.payload)
        if (payload.expiresAt <= now()) {
            log.d { "Ignored expired mesh receipt" }
            return false
        }
        val row = storage.get(ownerId, payload.packetHash) ?: run {
            log.d { "Ignored mesh receipt without a matching local packet" }
            return false
        }
        if (row.receipt != null) {
            log.d { "Ignored duplicate mesh receipt (message=${row.messageId})" }
            return false
        }
        if (row.messageId != payload.messageId || row.expiresAt != payload.expiresAt) {
            log.e(message = "Rejected mesh receipt with mismatched metadata (message=${row.messageId})")
            return false
        }
        val rawPacket = row.packet ?: return false
        val packet = json.decodeFromString<SignedMeshData>(rawPacket)
        val message = json.decodeFromString<MeshMessagePayload>(packet.payload)
        if (message.authorId != payload.authorId || message.message.chatId != payload.chatId ||
            !verifyRsa(
                data = RECEIPT_SIGNATURE_PREFIX + receipt.payload,
                signature = receipt.signature,
                publicKey = key,
            )
        ) {
            log.e(message = "Rejected mesh receipt with invalid signature or metadata (message=${row.messageId})")
            return false
        }
        if (row.outgoing != 0L) {
            outgoing.sendDeleteMutex.withLock {
                if (userStorage.getUserId() != ownerId) return false
                if (chatStorage.isMessageDeletionPending(
                        chatId = payload.chatId,
                        messageId = row.messageId,
                        authorId = ownerId,
                    )
                ) {
                    log.d { "Ignored mesh receipt for message=${row.messageId} awaiting deletion" }
                    return false
                }
                chatStorage.markMessageSent(
                    id = row.messageId,
                    acceptedRecipientIds = payload.acceptedRecipientIds,
                )
                log.d { "Marked outgoing mesh message=${row.messageId} as sent" }
            }
        }
        storage.saveReceipt(
            ownerId = ownerId,
            packetHash = payload.packetHash,
            receipt = json.encodeToString(receipt),
        )
        log.d {
            "Verified and saved mesh receipt (message=${row.messageId}, packet=${
                row.packetHash.take(
                    12,
                )
            }, " +
                    "outgoing=${row.outgoing != 0L}, acceptedRecipients=${payload.acceptedRecipientIds.size})"
        }
        return true
    }

    private suspend fun getReceiptKey(): String? {
        settings.getString(RECEIPT_KEY)?.let { return it }
        return attempt("fetching the mesh receipt key") {
            log.d { "Fetching the server key for mesh receipt verification" }
            api.getReceiptKey().also {
                settings.putString(RECEIPT_KEY, it)
                log.d { "Cached the server key for mesh receipt verification" }
            }
        }
    }

    private suspend fun scheduleExchangeRetry() {
        attempt("scheduling periodic BLE exchange") {
            schedulingLock.withLock {
                val input = currentInput() ?: return@withLock
                backgroundWork.enqueue(
                    key = MESH_EXCHANGE_RETRY,
                    worker = ExchangeMeshMessagesWorker::class,
                    conditions = BackgroundWorkConditions(
                        schedule = BackgroundWorkSchedule.Repeatable(15.minutes),
                        requiresInternet = false,
                    ),
                    type = Type.Silent,
                    policy = BackgroundWorkPolicy.KEEP,
                    priority = BackgroundWorkPriority.Normal,
                    input = input + (MESH_RETRY_EXCHANGE_INPUT to "true"),
                )
                log.d { "Scheduled periodic BLE exchange (key=$MESH_EXCHANGE_RETRY)" }
            }
        }
    }

    private suspend fun scheduleUpload() {
        attempt("scheduling mesh upload") {
            schedulingLock.withLock {
                val input = currentInput() ?: return@withLock
                enqueue(online = true, input = input)
                backgroundWork.enqueue(
                    key = MESH_UPLOAD_RETRY,
                    worker = UploadMeshMessagesWorker::class,
                    conditions = BackgroundWorkConditions(
                        schedule = BackgroundWorkSchedule.Repeatable(15.minutes),
                        requiresInternet = true,
                    ),
                    type = Type.Silent,
                    policy = BackgroundWorkPolicy.KEEP,
                    priority = BackgroundWorkPriority.Normal,
                    input = input,
                )
            }
        }
    }

    private suspend fun enqueue(online: Boolean, input: Map<String, String>) {
        val key = if (online) MESH_UPLOAD_WORK else MESH_EXCHANGE_WORK
        backgroundWork.enqueueOneShot(
            key = key,
            worker = if (online) UploadMeshMessagesWorker::class else ExchangeMeshMessagesWorker::class,
            requiresInternet = online,
            type = Type.Silent,
            policy = BackgroundWorkPolicy.KEEP_PENDING,
            priority = BackgroundWorkPriority.Expedited(legacyForeground = Type.Silent),
            input = input,
        ).await()
        log.d { "Requested background work (key=$key, requiresInternet=$online)" }
    }

    private suspend fun currentInput(): Map<String, String>? {
        val userId = userStorage.getUserId() ?: return null
        val originDeviceId = userStorage.getDeviceId() ?: return null
        return mapOf(MESH_USER_ID to userId, MESH_AUTH_DEVICE_ID to originDeviceId)
    }

    private suspend fun isCurrentSession(ownerId: String, originDeviceId: String): Boolean {
        return userStorage.getUserId() == ownerId && userStorage.getDeviceId() == originDeviceId
    }

    private fun now(): Long = timeProvider.now().toEpochMilliseconds()

    private fun MeshPacketRow.isExpired(now: Long): Boolean {
        return minOf(expiresAt, relayExpiresAt ?: expiresAt) <= now
    }

    private suspend fun removeExpiredPackets(now: Long) {
        val removed = storage.deleteExpired(now)
        if (removed > 0) {
            log.d { "Cleaned up expired mesh packets and receipts (count=$removed)" }
        }
    }

    private fun MessageSM.hasSameMeshRecipients(payload: MeshMessagePayload): Boolean {
        val recipientIds = recipients.mapTo(mutableSetOf()) { it.userId }
        recipientIds.add(authorId)
        return recipientIds == payload.message.recipients.mapTo(mutableSetOf()) { it.recipientId }
    }

    private suspend fun <T> attempt(action: String, block: suspend () -> T): T? {
        return try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            if (error !is Exception && error !is AppError) throw error
            val reason =
                if (error is NetworkError) "HTTP ${error.statusCode}" else error::class.simpleName
            log.e(message = "Mesh error while $action ($reason)")
            null
        }
    }

    private suspend fun sendAcknowledgement(acknowledgement: PendingPacketAcknowledgement) {
        val (peer, peerId) = acknowledgement.target
        controlFrameLock.withLock {
            val active = stateLock.withLock {
                meshDeviceId == acknowledgement.localDeviceId &&
                        peerHellos[peer]?.deviceId == peerId &&
                        isCurrentSession(acknowledgement.ownerId, acknowledgement.originDeviceId)
            }
            if (!active) return
            sendHello(peer, acknowledgement.localDeviceId)
            val frame = MeshFrame.PacketAcknowledgement(acknowledgement.packetHash)
            repo.send(peer = peer, text = json.encodeToString<MeshFrame>(frame))
            log.d {
                "Acknowledged received BLE packet (session=${peer.sessionId}, " +
                        "packet=${acknowledgement.packetHash.take(12)})"
            }
        }
    }

    private fun getOrCreateMeshDeviceId(): String {
        val cached = meshDeviceId
        if (cached != null) return cached
        val stored = settings.getString(DEVICE_ID_KEY) ?: Uuid.random().toString().also {
            settings.putString(DEVICE_ID_KEY, it)
            log.d { "Created a local mesh device ID" }
        }
        meshDeviceId = stored
        return stored
    }

    private class ReceivedPacket(val packetHash: String, val queueChanged: Boolean)

    private data class ConnectedPeer(val peer: MeshPeer, val deviceId: String)

    private sealed interface ExchangeTransfer {
        val target: ConnectedPeer

        data class Message(override val target: ConnectedPeer, val row: MeshPacketRow) :
            ExchangeTransfer

        data class Receipts(override val target: ConnectedPeer, val rows: List<MeshPacketRow>) :
            ExchangeTransfer
    }

    private class PendingPacketAcknowledgement(
        val ownerId: String,
        val originDeviceId: String,
        val target: ConnectedPeer,
        val localDeviceId: String,
        val packetHash: String,
    )
}

private const val DEVICE_ID_KEY = "mesh.deviceId"
private const val RECEIPT_KEY = "mesh.serverReceiptKey"
private const val RECEIPTS_PER_FRAME = 8
private val RELAY_RETENTION = 1.days.inWholeMilliseconds
private val PACKET_HASH_PATTERN = Regex("[0-9a-f]{64}")
internal const val MESH_EXCHANGE_WORK = "mesh.exchange-messages"
internal const val MESH_EXCHANGE_RETRY = "mesh.retry-exchange-messages"
internal const val MESH_RETRY_EXCHANGE_INPUT = "retryExchange"
internal const val MESH_UPLOAD_WORK = "mesh.upload-messages"
internal const val MESH_UPLOAD_RETRY = "mesh.retry-upload-messages"
internal const val MESH_USER_ID = "userId"
internal const val MESH_AUTH_DEVICE_ID = "authDeviceId"
