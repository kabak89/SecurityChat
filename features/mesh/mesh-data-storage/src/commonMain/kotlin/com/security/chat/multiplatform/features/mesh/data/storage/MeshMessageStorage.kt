package com.security.chat.multiplatform.features.mesh.data.storage

import com.security.chat.multiplatform.common.core.db.DatabaseCreator
import com.security.chat.multiplatform.common.core.db.SecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import kotlinx.coroutines.withContext

public class MeshMessageStorage(
    private val dispatcherProvider: DispatcherProviderInterface,
    driverFactory: SecuredDatabaseDriverFactory,
) {

    private val dbCreator = DatabaseCreator(dispatcherProvider) {
        MeshMessagesDb(
            driver = driverFactory.createDriver(
                databaseName = "mesh-messages.db",
                sqlSchema = MeshMessagesDb.Schema,
                version = MeshMessagesDb.Schema.version.toInt(),
            ),
        )
    }

    public suspend fun save(packet: MeshPacketRow, sourceId: String?) {
        withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.transaction {
                db.meshPacketRowQueries.insertPacket(packet)
                if (sourceId != null) {
                    db.meshPeerPacketRowQueries.rememberSource(packet.packetHash, sourceId)
                }
            }
        }
    }

    public suspend fun get(ownerId: String, packetHash: String): MeshPacketRow? {
        return withContext(dispatcherProvider.IO) {
            dbCreator.getDb().meshPacketRowQueries.getPacket(ownerId, packetHash)
                .executeAsOneOrNull()
        }
    }

    public suspend fun getOutgoing(ownerId: String, messageId: String): MeshPacketRow? {
        return withContext(dispatcherProvider.IO) {
            dbCreator.getDb().meshPacketRowQueries
                .getOutgoing(ownerId, messageId).executeAsOneOrNull()
        }
    }

    public suspend fun getPackets(ownerId: String, now: Long): List<MeshPacketRow> {
        return withContext(dispatcherProvider.IO) {
            dbCreator.getDb().meshPacketRowQueries.getPackets(ownerId, now).executeAsList()
        }
    }

    public suspend fun getSources(packetHash: String): List<String> {
        return withContext(dispatcherProvider.IO) {
            dbCreator.getDb().meshPeerPacketRowQueries.getSources(packetHash).executeAsList()
        }
    }

    public suspend fun countRelayPackets(ownerId: String, now: Long): Long {
        return withContext(dispatcherProvider.IO) {
            dbCreator.getDb().meshPacketRowQueries.countRelayPackets(ownerId, now).executeAsOne()
        }
    }

    public suspend fun saveReceipt(ownerId: String, packetHash: String, receipt: String) {
        withContext(dispatcherProvider.IO) {
            dbCreator.getDb().meshPacketRowQueries.saveReceipt(
                receipt = receipt,
                ownerId = ownerId,
                packetHash = packetHash,
            )
        }
    }

    public suspend fun finishDisplay(ownerId: String, packetHash: String) {
        withContext(dispatcherProvider.IO) {
            dbCreator.getDb().meshPacketRowQueries.finishDisplay(ownerId, packetHash)
        }
    }

    public suspend fun deleteExpired(now: Long): Long {
        return withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.transactionWithResult {
                val received = db.meshPacketRowQueries.deleteExpired(now)
                val relayed = db.meshPacketRowQueries.discardExpiredRelayPackets(now)
                val outgoing = db.meshPacketRowQueries.discardExpiredOutgoing(now)
                db.meshPeerPacketRowQueries.deleteOrphanSources(now)
                db.meshPeerTransferRowQueries.deleteExpiredTransfers(now)
                received + relayed + outgoing
            }
        }
    }

    public suspend fun discard(ownerId: String, packetHash: String) {
        withContext(dispatcherProvider.IO) {
            dbCreator.getDb().meshPacketRowQueries.discardPacket(ownerId, packetHash)
        }
    }

    public suspend fun clear() {
        withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.transaction {
                db.meshPeerPacketRowQueries.clearSources()
                db.meshPeerTransferRowQueries.clearTransfers()
                db.meshPacketRowQueries.clearPackets()
            }
        }
    }

    public suspend fun rememberTransfer(
        ownerId: String,
        peerId: String,
        packetHash: String,
        now: Long,
    ): Boolean {
        return withContext(dispatcherProvider.IO) {
            dbCreator.getDb().meshPeerTransferRowQueries.rememberTransfer(
                peerId = peerId,
                ownerId = ownerId,
                packetHash = packetHash,
                now = now,
            ) > 0
        }
    }

    public suspend fun hasTransfer(peerId: String, packetHash: String): Boolean {
        return withContext(dispatcherProvider.IO) {
            dbCreator.getDb().meshPeerTransferRowQueries.hasTransfer(peerId, packetHash)
                .executeAsOne()
        }
    }
}
