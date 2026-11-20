package com.security.chat.multiplatform.features.mesh.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.security.chat.multiplatform.common.background.work.BackgroundWorkConditions
import com.security.chat.multiplatform.common.background.work.BackgroundWorkManager
import com.security.chat.multiplatform.common.background.work.BackgroundWorkSchedule
import com.security.chat.multiplatform.common.core.db.DestructiveMigrationSchema
import com.security.chat.multiplatform.common.core.db.SecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.network.ConnectivityObserver
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.core.time.TimeProvider
import com.security.chat.multiplatform.common.encryption.generateKeysPair
import com.security.chat.multiplatform.common.encryption.signRsa
import com.security.chat.multiplatform.common.settings.PublicSettings
import com.security.chat.multiplatform.features.chat.data.common.ChatDataHelper
import com.security.chat.multiplatform.features.chat.data.common.ChatEventsPublisher
import com.security.chat.multiplatform.features.chat.data.common.OutgoingMessageScheduler
import com.security.chat.multiplatform.features.chat.data.network.entity.network.RecipientCiphertext
import com.security.chat.multiplatform.features.chat.data.network.entity.network.TextMessageRequest
import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageRecipientSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.Status
import com.security.chat.multiplatform.features.chats.data.common.ChatsDataHelper
import com.security.chat.multiplatform.features.chats.data.storage.ChatsStorage
import com.security.chat.multiplatform.features.mesh.data.storage.MeshMessageStorage
import com.security.chat.multiplatform.features.mesh.data.storage.MeshMessagesDb
import com.security.chat.multiplatform.features.mesh.data.storage.MeshPacketRow
import com.security.chat.multiplatform.features.mesh.domain.LogManager
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshIncomingPacket
import com.security.chat.multiplatform.features.mesh.domain.entity.MeshPeer
import com.security.chat.multiplatform.features.mesh.domain.repo.MeshRepo
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import com.security.chat.multiplatform.features.users.data.common.UsersDataHelper
import com.security.chat.multiplatform.features.users.data.storage.UsersStorage
import com.security.chat.multiplatform.features.users.data.storage.entity.UserSM
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.Uuid

class MeshMessageServiceTest {

    @Test
    fun `BLE exchanges remember peer acknowledgements and use signed receipts for message status`() =
        runTest {
            JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
                MeshMessagesDb.Schema.create(driver).await()
                val dispatcher = StandardTestDispatcher(testScheduler)
                val storage = MeshMessageStorage(
                    dispatcherProvider = object : DispatcherProviderInterface {
                        override val Default = dispatcher
                        override val IO = dispatcher
                        override val Unconfined = dispatcher
                        override val Main = Dispatchers.Main
                    },
                    driverFactory = stub<SecuredDatabaseDriverFactory> { _, _ -> driver },
                )
                val ownerId = Uuid.random().toString()
                val deviceId = Uuid.random().toString()
                val authorId = Uuid.random().toString()
                val chatId = Uuid.random().toString()
                val recipientId = Uuid.random().toString()
                val messageId = Uuid.random().toString()
                var now = 1_000L
                val server = generateKeysPair()
                val author = generateKeysPair()
                val localId = Uuid.random().toString()
                val json = Json
                val settings = meshSettings(localId, server.publicKey)
                val signedInUser = MutableStateFlow<String?>(ownerId)
                val user = stub<UserStorage> { method, _ ->
                    when (method) {
                        "getSessionUserId" -> signedInUser
                        "getUserId" -> signedInUser.value
                        "getDeviceId" -> deviceId.takeIf { signedInUser.value != null }
                        "getKeys" -> author
                        else -> error(method)
                    }
                }
                val users = stub<UsersStorage> { method, args ->
                    check(method == "getUser")
                    if (args[0] == recipientId) {
                        UserSM(id = recipientId, publicKey = author.publicKey, name = "Recipient")
                    } else {
                        null
                    }
                }
                val repo = TestRepo()
                val logs = mutableListOf<String>()
                val markedSent = mutableListOf<Pair<String, List<*>>>()
                val ownMessages = mutableMapOf<String, MessageSM.Text>()
                val savedMessages = MutableSharedFlow<String>()
                val cancelledWork = mutableListOf<String>()
                var periodicInput: Map<String, String>? = null
                val online = MutableStateFlow(true)
                val log = stub<LogManager> { method, args ->
                    logs += if (method == "d") {
                        (args[0] as Function0<*>).invoke().toString()
                    } else {
                        args[0].toString()
                    }
                    Unit
                }
                val outgoing = stub<OutgoingMessageScheduler> { method, _ ->
                    when (method) {
                        "getSavedMessages" -> savedMessages
                        "getSendDeleteMutex" -> Mutex()
                        else -> error(method)
                    }
                }
                val service = MeshMessageService(
                    repo = repo,
                    storage = storage,
                    chatStorage = stub<ChatStorage> { method, args ->
                        when (method) {
                            "migrateMessageStatuses" -> Unit
                            "getOutgoingTextForMesh" -> ownMessages.values.toList()
                            "getMessage" -> ownMessages[args[1]]
                            "isMessageDeletionPending" -> false
                            "markMessageSent" -> {
                                markedSent += (args[0] as String) to (args[1] as List<*>)
                                Unit
                            }

                            else -> error(method)
                        }
                    },
                    chatsStorage = stub<ChatsStorage> { method, _ -> error(method) },
                    chats = stub<ChatsDataHelper> { method, _ -> error(method) },
                    usersStorage = users,
                    users = stub<UsersDataHelper> { method, _ -> error(method) },
                    userStorage = user,
                    outgoing = outgoing,
                    chatEvents = stub<ChatEventsPublisher> { method, _ -> error(method) },
                    notifier = MeshIncomingNotifier { error("Unexpected local notification") },
                    settings = settings,
                    backgroundWork = stub<BackgroundWorkManager> { method, args ->
                        when (method) {
                            "enqueueOneShot" -> CompletableDeferred(Unit)
                            "enqueue" -> {
                                if (args[0] == MESH_EXCHANGE_RETRY) {
                                    assertEquals(ExchangeMeshMessagesWorker::class, args[1])
                                    val conditions = args[2] as BackgroundWorkConditions
                                    assertFalse(conditions.requiresInternet)
                                    assertEquals(
                                        BackgroundWorkSchedule.Repeatable(15.minutes),
                                        conditions.schedule,
                                    )
                                    periodicInput = (args[6] as Map<*, *>).entries.associate {
                                        it.key as String to it.value as String
                                    }
                                }
                                Unit
                            }

                            "cancel" -> {
                                cancelledWork += args[0] as String
                                Unit
                            }

                            else -> error(method)
                        }
                    },
                    connectivity = object : ConnectivityObserver {
                        override val isOnline = online
                    },
                    crypto = MeshMessageCrypto(
                        chatDataHelper = stub<ChatDataHelper> { method, _ ->
                            when (method) {
                                "getOneTimeEncryptionKey", "encryptKey" -> "encrypted-key"
                                "encryptText" -> "encrypted-message"
                                else -> error(method)
                            }
                        },
                        userStorage = user,
                        usersStorage = users,
                        json = json,
                        log = log,
                    ),
                    api = MeshRelayApi(
                        networkManagerFactory = stub<NetworkManagerFactory> { method, _ ->
                            error(
                                method,
                            )
                        },
                        networkConfig = NetworkConfig(host = "https://relay.test", port = 443),
                    ),
                    timeProvider = stub<TimeProvider> { _, _ -> Instant.fromEpochMilliseconds(now) },
                    json = json,
                    log = log,
                )
                service.start(backgroundScope)
                val worker = ExchangeMeshMessagesWorker(service)
                val workInput = mapOf(MESH_USER_ID to ownerId, MESH_AUTH_DEVICE_ID to deviceId)
                val source = MeshPeer("source")
                val next = MeshPeer("next")
                repo.ready.value = listOf(source, next)
                val sourceId = Uuid.random().toString()
                val nextId = Uuid.random().toString()
                val nextHello = MeshFrame.Hello(nextId, supportsPacketAcknowledgements = true)
                repo.incoming.send(
                    frame(
                        source,
                        MeshFrame.Hello(sourceId, supportsPacketAcknowledgements = true),
                    ),
                )
                repo.incoming.send(frame(next, nextHello))
                val message = MeshMessagePayload(
                    authorId = authorId,
                    authorDeviceId = Uuid.random().toString(),
                    createdAt = now,
                    expiresAt = now + PACKET_DURATION,
                    message = TextMessageRequest(
                        id = messageId,
                        chatId = chatId,
                        timestamp = now,
                        recipients = listOf(RecipientCiphertext(recipientId, "encrypted-key")),
                        message = "encrypted-message",
                        replyToMessageId = null,
                    ),
                )
                val messagePayload = json.encodeToString(message)
                val packet = SignedMeshData(
                    payload = messagePayload,
                    signature = signRsa(
                        data = MESSAGE_SIGNATURE_PREFIX + messagePayload,
                        privateKey = author.privateKey,
                    ),
                )
                repo.incoming.send(frame(source, MeshFrame.Message(packet)))
                runCurrent()
                val packetAck = MeshFrame.PacketAcknowledgement(packet.packetHash())
                assertTrue(source to packetAck in repo.sent)
                repo.incoming.send(frame(source, MeshFrame.Message(packet)))
                runCurrent()
                assertEquals(2, repo.sent.count { it == source to packetAck })
                worker.run(workInput)
                assertEquals(
                    listOf(next to MeshFrame.Message(packet)),
                    repo.sent.filter { it.second is MeshFrame.Message },
                )
                val retryInput = assertNotNull(periodicInput)
                repo.incoming.send(frame(next, packetAck))
                runCurrent()
                assertTrue(storage.hasTransfer(nextId, packet.packetHash()))
                worker.run(retryInput)
                assertEquals(1, repo.sent.count { it.second is MeshFrame.Message })
                online.value = false

                val outgoingId = Uuid.random().toString()
                val outgoingPayload = json.encodeToString(
                    message.copy(
                        authorId = ownerId,
                        authorDeviceId = deviceId,
                        message = message.message.copy(
                            id = outgoingId,
                            recipients = message.message.recipients + RecipientCiphertext(
                                ownerId,
                                "author-key",
                            ),
                        ),
                    ),
                )
                val outgoingPacket = SignedMeshData(
                    payload = outgoingPayload,
                    signature = signRsa(
                        data = MESSAGE_SIGNATURE_PREFIX + outgoingPayload,
                        privateKey = author.privateKey,
                    ),
                )
                val outgoingHash = outgoingPacket.packetHash()
                storage.save(
                    packet = MeshPacketRow(
                        packetHash = outgoingHash,
                        ownerId = ownerId,
                        messageId = outgoingId,
                        expiresAt = message.expiresAt,
                        outgoing = 1,
                        packet = json.encodeToString(outgoingPacket),
                        receipt = null,
                        pendingDisplay = 0,
                        relayExpiresAt = null,
                    ),
                    sourceId = null,
                )
                val relayedReceipt = MeshReceiptPayload(
                    authorId = authorId,
                    chatId = chatId,
                    messageId = messageId,
                    packetHash = packet.packetHash(),
                    expiresAt = message.expiresAt,
                    acceptedRecipientIds = listOf(authorId, recipientId),
                )
                val acceptedRecipientIds = listOf(ownerId, recipientId)
                val receiptPayload = json.encodeToString(relayedReceipt)
                val outgoingReceiptPayload = json.encodeToString(
                    relayedReceipt.copy(
                        authorId = ownerId,
                        messageId = outgoingId,
                        packetHash = outgoingHash,
                        acceptedRecipientIds = acceptedRecipientIds,
                    ),
                )
                val forged = SignedMeshData(payload = receiptPayload, signature = "AAAA")
                val forgedOutgoing = forged.copy(payload = outgoingReceiptPayload)
                repo.incoming.send(frame(next, MeshFrame.Receipts(listOf(forged, forgedOutgoing))))
                runCurrent()
                assertNull(storage.get(ownerId, packet.packetHash())?.receipt)
                assertNull(storage.get(ownerId, outgoingHash)?.receipt)
                assertTrue(markedSent.isEmpty())

                val signed = SignedMeshData(
                    payload = receiptPayload,
                    signature = signRsa(
                        data = RECEIPT_SIGNATURE_PREFIX + receiptPayload,
                        privateKey = server.privateKey,
                    ),
                )
                val signedOutgoing = SignedMeshData(
                    payload = outgoingReceiptPayload,
                    signature = signRsa(
                        data = RECEIPT_SIGNATURE_PREFIX + outgoingReceiptPayload,
                        privateKey = server.privateKey,
                    ),
                )
                repo.incoming.send(frame(next, MeshFrame.Receipts(listOf(signed, signedOutgoing))))
                runCurrent()
                val row = assertNotNull(storage.get(ownerId, packet.packetHash()))
                assertNull(row.packet)
                assertNotNull(row.receipt)
                val acceptedOutgoing = assertNotNull(storage.get(ownerId, outgoingHash))
                assertNotNull(acceptedOutgoing.receipt)
                assertNotNull(acceptedOutgoing.packet)
                val expectedStatusUpdates =
                    listOf<Pair<String, List<*>>>(outgoingId to acceptedRecipientIds)
                assertEquals(expectedStatusUpdates, markedSent)

                repo.incoming.send(frame(next, MeshFrame.Receipts(listOf(signedOutgoing))))
                runCurrent()
                assertEquals(expectedStatusUpdates, markedSent)

                val reconnected = MeshPeer("source reconnected")
                repo.ready.value = listOf(reconnected, next)
                repo.incoming.send(frame(reconnected, MeshFrame.Hello(sourceId)))
                runCurrent()
                worker.run(workInput)
                assertEquals(
                    listOf(reconnected to MeshFrame.Receipts(listOf(signed))),
                    repo.sent.filter { it.second is MeshFrame.Receipts },
                )
                assertEquals(1, repo.sent.count { it.second is MeshFrame.Message })

                val pendingRecipient =
                    MessageRecipientSM(userId = recipientId, status = Status.Sent)
                val acceptedMessage = MessageSM.Text(
                    id = outgoingId,
                    chatId = chatId,
                    recipients = listOf(pendingRecipient),
                    authorId = ownerId,
                    timestamp = now,
                    text = "local text",
                )
                val sentViaNetwork = acceptedMessage.copy(id = Uuid.random().toString())
                ownMessages[outgoingId] = acceptedMessage
                ownMessages[sentViaNetwork.id] = sentViaNetwork
                online.value = true
                repo.ready.value = listOf(next)
                runCurrent()
                repo.sent.clear()
                worker.run(workInput)
                val packetsSentOnline = repo.sent.mapNotNull { it.second as? MeshFrame.Message }
                assertEquals(
                    listOf(outgoingId, sentViaNetwork.id),
                    packetsSentOnline.map { json.decodeFromString<MeshMessagePayload>(it.packet.payload).message.id },
                )
                assertEquals(outgoingPacket, packetsSentOnline.first().packet)

                repo.sent.clear()
                worker.run(workInput)
                assertTrue(repo.sent.none { it.second is MeshFrame.Message })

                ownMessages[outgoingId] = acceptedMessage.copy(
                    recipients = listOf(pendingRecipient.copy(status = Status.ReceivedByReceiver)),
                )
                val finishSending = CompletableDeferred<Unit>()
                repo.beforeSend = { if (it is MeshFrame.Message) finishSending.await() }
                val retry = async { worker.run(retryInput) }
                runCurrent()
                assertFalse(retry.isCompleted)
                val beforeReceipt = logs.size
                repo.incoming.send(frame(next, MeshFrame.Receipts(listOf(signed))))
                runCurrent()
                assertTrue(
                    logs.drop(beforeReceipt)
                        .any { "Ignored duplicate mesh receipt (message=$messageId)" in it },
                )
                finishSending.complete(Unit)
                retry.await()
                assertEquals(
                    listOf(next to packetsSentOnline.last()),
                    repo.sent.filter { it.second is MeshFrame.Message },
                )

                val unconfirmedMessage = packetsSentOnline.last()
                val confirmedHash = unconfirmedMessage.packet.packetHash()
                repo.incoming.send(frame(next, MeshFrame.PacketAcknowledgement(confirmedHash)))
                runCurrent()
                assertTrue(storage.hasTransfer(nextId, confirmedHash))
                assertEquals(expectedStatusUpdates, markedSent)
                repo.sent.clear()
                worker.run(retryInput)
                assertTrue(repo.sent.none { it.second is MeshFrame.Message })

                assertPeerWithNewIdGetsPacket(
                    repo = repo,
                    peer = next,
                    packet = unconfirmedMessage,
                    runExchange = { worker.run(workInput) },
                )
                repo.sent.clear()
                val nextReconnected = MeshPeer("next reconnected")
                val anotherPeer = MeshPeer("another peer")
                repo.ready.value = listOf(nextReconnected, anotherPeer)
                runCurrent()
                repo.incoming.send(frame(nextReconnected, nextHello))
                repo.incoming.send(frame(anotherPeer, MeshFrame.Hello(Uuid.random().toString())))
                runCurrent()
                worker.run(workInput)
                assertEquals(
                    listOf(anotherPeer to unconfirmedMessage),
                    repo.sent.filter { it.second is MeshFrame.Message },
                )
                repo.ready.value = listOf(next)
                runCurrent()
                repo.incoming.send(frame(next, nextHello))

                val waitingMessage =
                    message.copy(message = message.message.copy(id = Uuid.random().toString()))
                val waitingPayload = json.encodeToString(waitingMessage)
                val waitingPacket = SignedMeshData(
                    payload = waitingPayload,
                    signature = signRsa(
                        data = MESSAGE_SIGNATURE_PREFIX + waitingPayload,
                        privateKey = author.privateKey,
                    ),
                )
                val deadline = now + 1.days.inWholeMilliseconds
                assertRelayPacketExpires(
                    storage = storage,
                    ownerId = ownerId,
                    repo = repo,
                    source = next to nextHello,
                    secondSource = reconnected to MeshFrame.Hello(sourceId),
                    packet = waitingPacket,
                    deadline = deadline,
                    setTime = { now = it },
                    runExchange = { worker.run(retryInput) },
                )
                assertNotNull(storage.get(ownerId, packet.packetHash())?.receipt)
                assertNotNull(storage.get(ownerId, outgoingHash)?.packet)
                assertTrue(logs.any { "Ignored mesh packet past its local relay deadline" in it })

                assertNewPacketsInterleaveWithBacklog(
                    repo = repo,
                    peer = next,
                    hello = nextHello,
                    ownMessages = ownMessages,
                    savedMessages = savedMessages,
                    template = acceptedMessage.copy(timestamp = now),
                    runExchange = { worker.run(workInput) },
                )

                signedInUser.value = null
                service.clear()
                assertNull(settings.getString("mesh.deviceId"))
                assertTrue(MESH_EXCHANGE_RETRY in cancelledWork)
                assertNull(storage.get(ownerId, packet.packetHash()))
                assertFalse(storage.hasTransfer(nextId, confirmedHash))
                ownMessages.clear()
                signedInUser.value = ownerId
                assertNewLocalIdIsAnnounced(
                    repo = repo,
                    peer = next,
                    settings = settings,
                    oldId = localId,
                    runExchange = { worker.run(workInput) },
                )
                assertTrue(logs.any { "Mesh worker started (key=$MESH_EXCHANGE_WORK)" in it })
                for (event in listOf(
                    "Queued received mesh packet",
                    "Sent mesh packet over BLE",
                    "Verified and saved mesh receipt",
                    "Returned mesh receipts over BLE",
                )) {
                    assertTrue(logs.any { event in it && messageId in it }, event)
                }
                assertFalse(logs.any { it.contains("encrypted-message") || it.contains(packet.signature) })
            }
        }

    @Test
    fun `upgrading mesh storage sets a relay deadline and preserves outgoing packets`() = runTest {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            val createdAt = 1_000L
            val expiresAt = createdAt + PACKET_DURATION
            val statements = listOf(
                """
                    CREATE TABLE MeshPacketRow (
                        packetHash TEXT NOT NULL PRIMARY KEY,
                        ownerId TEXT NOT NULL,
                        messageId TEXT NOT NULL,
                        expiresAt INTEGER NOT NULL,
                        outgoing INTEGER NOT NULL,
                        packet TEXT,
                        receipt TEXT,
                        pendingDisplay INTEGER NOT NULL
                    )
                """.trimIndent(),
                """
                    INSERT INTO MeshPacketRow VALUES
                    ('received', 'owner', 'received-message', $expiresAt, 0, 'received-packet', NULL, 1),
                    ('outgoing', 'owner', 'outgoing-message', $expiresAt, 1, 'outgoing-packet', NULL, 0)
                """.trimIndent(),
            )
            for (sql in statements) {
                driver.execute(identifier = null, sql = sql, parameters = 0).await()
            }
            val schema =
                DestructiveMigrationSchema(MeshMessagesDb.Schema, MeshMessagesDb.Schema.version)
            schema.migrate(driver = driver, oldVersion = 1, newVersion = schema.version).await()

            val db = MeshMessagesDb(driver)
            val queries = db.meshPacketRowQueries
            val received = queries.getPacket("owner", "received").executeAsOne()
            assertEquals(createdAt + 1.days.inWholeMilliseconds, received.relayExpiresAt)
            assertEquals("received-packet", received.packet)
            val outgoing = queries.getPacket("owner", "outgoing").executeAsOne()
            assertNull(outgoing.relayExpiresAt)
            assertEquals("outgoing-packet", outgoing.packet)
            val transfers = db.meshPeerTransferRowQueries
            assertEquals(
                1L,
                transfers.rememberTransfer(
                    peerId = "peer",
                    ownerId = "owner",
                    packetHash = "outgoing",
                    now = createdAt,
                ),
            )
            assertTrue(transfers.hasTransfer("peer", "outgoing").executeAsOne())
            transfers.deleteExpiredTransfers(expiresAt)
            assertFalse(transfers.hasTransfer("peer", "outgoing").executeAsOne())
        }
    }

    private suspend fun TestScope.assertRelayPacketExpires(
        storage: MeshMessageStorage,
        ownerId: String,
        repo: TestRepo,
        source: Pair<MeshPeer, MeshFrame.Hello>,
        secondSource: Pair<MeshPeer, MeshFrame.Hello>,
        packet: SignedMeshData,
        deadline: Long,
        setTime: (Long) -> Unit,
        runExchange: suspend () -> Unit,
    ) {
        val hash = packet.packetHash()
        val (peer, hello) = source
        val (otherPeer, otherHello) = secondSource
        repo.incoming.send(frame(peer, MeshFrame.Message(packet)))
        runCurrent()
        assertEquals(deadline, storage.get(ownerId, hash)?.relayExpiresAt)

        setTime(deadline - 1)
        repo.ready.value = listOf(peer, otherPeer)
        repo.incoming.send(frame(otherPeer, otherHello))
        repo.incoming.send(frame(otherPeer, MeshFrame.Message(packet)))
        runCurrent()
        assertEquals(deadline, storage.get(ownerId, hash)?.relayExpiresAt)
        assertEquals(2, storage.getSources(hash).size)

        setTime(deadline)
        repo.ready.value = emptyList()
        runExchange()
        assertNull(assertNotNull(storage.get(ownerId, hash)).packet)
        assertTrue(storage.getSources(hash).isEmpty())
        assertFalse(storage.getPackets(ownerId, deadline).any { it.packetHash == hash })
        assertEquals(0L, storage.countRelayPackets(ownerId, deadline))

        repo.incoming.send(frame(peer, hello))
        repo.incoming.send(frame(peer, MeshFrame.Message(packet)))
        runCurrent()
        assertNull(assertNotNull(storage.get(ownerId, hash)).packet)
        assertTrue(storage.getSources(hash).isEmpty())
    }

    private suspend fun TestScope.assertNewPacketsInterleaveWithBacklog(
        repo: TestRepo,
        peer: MeshPeer,
        hello: MeshFrame.Hello,
        ownMessages: MutableMap<String, MessageSM.Text>,
        savedMessages: MutableSharedFlow<String>,
        template: MessageSM.Text,
        runExchange: suspend () -> Unit,
    ) {
        val olderMessages = List(2) { template.copy(id = Uuid.random().toString()) }
        val freshMessages = List(2) {
            template.copy(id = Uuid.random().toString(), timestamp = template.timestamp + 1L)
        }
        ownMessages.clear()
        olderMessages.forEach { ownMessages[it.id] = it }
        repo.ready.value = listOf(peer)
        runCurrent()
        repo.incoming.send(frame(peer, hello))
        runCurrent()
        repo.sent.clear()
        val firstPacketStarted = CompletableDeferred<Unit>()
        val finishFirstPacket = CompletableDeferred<Unit>()
        repo.beforeSend = { frame ->
            if (frame is MeshFrame.Message &&
                Json.decodeFromString<MeshMessagePayload>(frame.packet.payload).message.id == olderMessages.first().id
            ) {
                firstPacketStarted.complete(Unit)
                finishFirstPacket.await()
            }
        }
        val exchange = async { runExchange() }
        runCurrent()
        assertTrue(firstPacketStarted.isCompleted)
        for (message in freshMessages) {
            ownMessages[message.id] = message
            savedMessages.emit(message.authorId)
        }
        runCurrent()
        finishFirstPacket.complete(Unit)
        exchange.await()
        val actualOrder = repo.sent.mapNotNull { (_, frame) ->
            (frame as? MeshFrame.Message)?.let {
                Json.decodeFromString<MeshMessagePayload>(it.packet.payload).message.id
            }
        }
        assertEquals(
            listOf(
                olderMessages[0].id,
                freshMessages[0].id,
                olderMessages[1].id,
                freshMessages[1].id,
            ),
            actualOrder,
        )
    }

    private suspend fun TestScope.assertPeerWithNewIdGetsPacket(
        repo: TestRepo,
        peer: MeshPeer,
        packet: MeshFrame.Message,
        runExchange: suspend () -> Unit,
    ) {
        repo.sent.clear()
        val newId = Uuid.random().toString()
        repo.incoming.send(
            frame(
                peer,
                MeshFrame.Hello(newId, supportsPacketAcknowledgements = true),
            ),
        )
        runCurrent()
        runExchange()
        assertEquals(listOf(peer to packet), repo.sent.filter { it.second is MeshFrame.Message })
    }

    private suspend fun TestScope.assertNewLocalIdIsAnnounced(
        repo: TestRepo,
        peer: MeshPeer,
        settings: PublicSettings,
        oldId: String,
        runExchange: suspend () -> Unit,
    ) {
        runCurrent()
        repo.sent.clear()
        runExchange()
        runExchange()
        val newId = assertNotNull(settings.getString("mesh.deviceId"))
        assertTrue(isUuid(newId))
        assertTrue(newId != oldId)
        val hellos = repo.sent.filter { it.second is MeshFrame.Hello }
        assertEquals(
            listOf(peer to MeshFrame.Hello(newId, supportsPacketAcknowledgements = true)),
            hellos,
        )
    }

    private fun meshSettings(deviceId: String, receiptKey: String): PublicSettings {
        val values = mutableMapOf<String, String?>(
            "mesh.deviceId" to deviceId,
            "mesh.serverReceiptKey" to receiptKey,
        )
        return stub<PublicSettings> { method, args ->
            when (method) {
                "getString" -> values[args[0] as String]
                "putString" -> {
                    values[args[0] as String] = args[1] as String?
                    Unit
                }

                else -> error(method)
            }
        }
    }

    private fun frame(peer: MeshPeer, frame: MeshFrame): MeshIncomingPacket {
        return MeshIncomingPacket(peer = peer, text = Json.encodeToString(frame))
    }

    private inline fun <reified T> stub(crossinline answer: (String, Array<out Any?>) -> Any?): T {
        return T::class.java.cast(
            Proxy.newProxyInstance(
                T::class.java.classLoader,
                arrayOf(T::class.java),
            ) { _, method, args ->
                answer(method.name, args.orEmpty())
            },
        )
    }

    private class TestRepo : MeshRepo {

        val ready = MutableStateFlow<List<MeshPeer>>(emptyList())
        val incoming = Channel<MeshIncomingPacket>(Channel.UNLIMITED)
        val sent = mutableListOf<Pair<MeshPeer, MeshFrame>>()
        var beforeSend: suspend (MeshFrame) -> Unit = {}

        override fun getReadyPeersFlow() = ready

        override fun getIncomingPacketsFlow() = incoming.receiveAsFlow()

        override suspend fun send(peer: MeshPeer, text: String) {
            val frame = Json.decodeFromString<MeshFrame>(text)
            beforeSend(frame)
            sent += peer to frame
        }
    }
}
