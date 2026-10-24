package com.security.chat.multiplatform.features.chat.data.storage

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.security.chat.multiplatform.common.core.db.DatabaseCreator
import com.security.chat.multiplatform.common.core.db.SecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chat.data.storage.entity.JoinedMessageRow
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageReceivedEventSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageRecipientSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.Status
import com.security.chat.multiplatform.features.chat.data.storage.mapper.mapStatusToString
import com.security.chat.multiplatform.features.chat.data.storage.mapper.mapStringToStatus
import com.security.chat.multiplatform.features.chat.data.storage.mapper.toImageTable
import com.security.chat.multiplatform.features.chat.data.storage.mapper.toMessageTable
import com.security.chat.multiplatform.features.chat.data.storage.mapper.toSM
import com.security.chat.multiplatform.features.chat.data.storage.mapper.toTextTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

public interface ChatStorage {
    public suspend fun migrateMessageStatuses(userId: String)
    public suspend fun saveMessage(message: MessageSM)
    public suspend fun saveMessages(messages: List<MessageSM>)
    public suspend fun saveReceivedMessages(messages: List<MessageSM>, receivedByUserId: String)
    public suspend fun enqueueStoredReceivedEvents(chatId: String, receivedByUserId: String)
    public fun observePendingReceivedEvents(receivedByUserId: String): Flow<List<MessageReceivedEventSM>>
    public suspend fun markReceivedEventPublished(id: String, receivedByUserId: String)
    public suspend fun getMessages(chatId: String, limit: Long, offset: Long): List<MessageSM>
    public suspend fun getNewestMessages(chatId: String, limit: Long): List<MessageSM>
    public suspend fun getOlderMessages(
        chatId: String,
        beforeTimestamp: Long,
        limit: Long,
    ): List<MessageSM>

    /**
     * Returns up to [limit] messages with `timestamp > afterTimestamp`, ordered by `timestamp ASC`
     * (closest-newer first). Use this for contiguous pagination toward newer messages: it never
     * leaves a gap between [afterTimestamp] and the returned items.
     */
    public suspend fun getClosestNewerMessages(
        chatId: String,
        afterTimestamp: Long,
        limit: Long,
    ): List<MessageSM>

    public fun observeMessagesChanges(chatId: String): Flow<Unit>
    public suspend fun updateMessage(message: MessageSM)
    public suspend fun markMessageSent(id: String)
    public suspend fun restoreOutgoingMessageRecipients(
        chatId: String,
        authorId: String,
        recipientUserIds: List<String>,
    )

    /** Returns true for a matching outgoing recipient, including an already processed delivery. */
    public suspend fun markMessageReceived(
        chatId: String,
        messageId: String,
        authorId: String,
        receivedByUserId: String,
    ): Boolean
    public suspend fun markMessagesReadUpTo(chatId: String, messageId: String, userId: String)
    public suspend fun clearAll()
}

internal class ChatStorageImpl(
    private val dispatcherProvider: DispatcherProviderInterface,
    private val driverFactory: SecuredDatabaseDriverFactory,
) : ChatStorage {

    private val dbCreator: DatabaseCreator<ChatDb> =
        DatabaseCreator(
            dispatcherProvider = dispatcherProvider,
            create = {
                ChatDb(
                    driver = driverFactory.createDriver(
                        databaseName = "chat.db",
                        sqlSchema = ChatDb.Schema,
                        version = 8,
                    ),
                )
            },
        )

    private val migrationMutex: Mutex = Mutex()
    private var migrationCompleted: Boolean = false

    override suspend fun saveMessage(message: MessageSM) {
        withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.transaction {
                db.persistMessage(message)
            }
        }
    }

    override suspend fun saveMessages(messages: List<MessageSM>) {
        withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.transaction {
                messages.forEach { message ->
                    db.persistMessage(message)
                }
            }
        }
    }

    override suspend fun getMessages(
        chatId: String,
        limit: Long,
        offset: Long,
    ): List<MessageSM> {
        return withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.attachRecipients(
                db.messageTableQueries
                    .getPaged(
                        chatId = chatId,
                        limit = limit,
                        offset = offset,
                        mapper = ::JoinedMessageRow,
                    )
                    .executeAsList(),
            )
        }
    }

    override suspend fun saveReceivedMessages(
        messages: List<MessageSM>,
        receivedByUserId: String,
    ) {
        if (messages.isEmpty()) return
        withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.transaction {
                messages.forEach { db.persistMessage(it) }
                db.messageReceivedEventsQueries.enqueue(
                    eventIdPrefix = "message.received:${receivedByUserId.length}:$receivedByUserId:",
                    receivedByUserId = receivedByUserId,
                    messageIds = messages.map { it.id },
                )
            }
        }
    }

    override suspend fun enqueueStoredReceivedEvents(chatId: String, receivedByUserId: String) {
        withContext(dispatcherProvider.IO) {
            dbCreator.getDb().messageReceivedEventsQueries.enqueueStored(
                eventIdPrefix = "message.received:${receivedByUserId.length}:$receivedByUserId:",
                receivedByUserId = receivedByUserId,
                chatId = chatId,
            )
        }
    }

    override fun observePendingReceivedEvents(
        receivedByUserId: String,
    ): Flow<List<MessageReceivedEventSM>> {
        return dbCreator.dbFlow
            .flatMapLatest { db ->
                db.messageReceivedEventsQueries.getPending(receivedByUserId)
                    .asFlow().mapToList(dispatcherProvider.IO)
            }
            .map { rows ->
                rows.map { row ->
                    MessageReceivedEventSM(
                        id = row.id,
                        messageId = row.messageId,
                        chatId = row.chatId,
                        recipientUserId = row.recipientUserId,
                        receivedByUserId = row.receivedByUserId,
                    )
                }
            }
            .flowOn(dispatcherProvider.IO)
    }

    override suspend fun markReceivedEventPublished(id: String, receivedByUserId: String) {
        withContext(dispatcherProvider.IO) {
            dbCreator.getDb().messageReceivedEventsQueries.markPublished(
                id = id,
                receivedByUserId = receivedByUserId,
            )
        }
    }

    override suspend fun getNewestMessages(chatId: String, limit: Long): List<MessageSM> {
        return withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.attachRecipients(
                db.messageTableQueries
                    .getNewest(
                        chatId = chatId,
                        limit = limit,
                        mapper = ::JoinedMessageRow,
                    )
                    .executeAsList(),
            )
        }
    }

    override suspend fun getOlderMessages(
        chatId: String,
        beforeTimestamp: Long,
        limit: Long,
    ): List<MessageSM> {
        return withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.attachRecipients(
                db.messageTableQueries
                    .getOlderThan(
                        chatId = chatId,
                        beforeTimestamp = beforeTimestamp,
                        limit = limit,
                        mapper = ::JoinedMessageRow,
                    )
                    .executeAsList(),
            )
        }
    }

    override suspend fun getClosestNewerMessages(
        chatId: String,
        afterTimestamp: Long,
        limit: Long,
    ): List<MessageSM> {
        return withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            val joinedMessageRows = db.messageTableQueries
                .getClosestNewerThan(
                    chatId = chatId,
                    afterTimestamp = afterTimestamp,
                    limit = limit,
                    mapper = ::JoinedMessageRow,
                )
                .executeAsList()

            db.attachRecipients(
                rows = joinedMessageRows,
            )
        }
    }

    override fun observeMessagesChanges(chatId: String): Flow<Unit> {
        return dbCreator.dbFlow
            .flatMapLatest { db ->
                combine(
                    db.messageTableQueries
                        .getPaged(
                            chatId = chatId,
                            limit = 1L,
                            offset = 0L,
                            mapper = ::JoinedMessageRow,
                        )
                        .asFlow(),
                    db.messageRecipientsQueries.observeChanges(chatId).asFlow(),
                ) { _, _ -> }
            }
            .flowOn(dispatcherProvider.IO)
    }

    override suspend fun updateMessage(message: MessageSM) {
        withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.transaction {
                db.messageTableQueries.insert(message.toMessageTable())
                db.insertMessageDetail(message)
            }
        }
    }

    override suspend fun markMessageSent(id: String) {
        withContext(dispatcherProvider.IO) {
            dbCreator.getDb().messageRecipientsQueries.markSent(messageId = id)
        }
    }

    override suspend fun restoreOutgoingMessageRecipients(
        chatId: String,
        authorId: String,
        recipientUserIds: List<String>,
    ) {
        if (recipientUserIds.isEmpty()) return
        withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.transaction {
                val messageIds = db.messageRecipientsQueries.getOutgoingWithoutOtherRecipients(
                    chatId = chatId,
                    authorId = authorId,
                ).executeAsList()
                for (messageId in messageIds) {
                    for (recipientUserId in recipientUserIds) {
                        db.messageRecipientsQueries.insert(
                            messageId = messageId,
                            userId = recipientUserId,
                            status = mapStatusToString(Status.Sent),
                        )
                    }
                }
            }
        }
    }

    override suspend fun markMessagesReadUpTo(
        chatId: String,
        messageId: String,
        userId: String,
    ) {
        withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.transaction {
                val unreadCount = db.messageRecipientsQueries.getUnreadCountUpTo(
                    userId = userId,
                    messageId = messageId,
                    chatId = chatId,
                ).executeAsOne()
                if (unreadCount > 0) {
                    db.messageRecipientsQueries.markReadUpTo(
                        userId = userId,
                        messageId = messageId,
                        chatId = chatId,
                    )
                }
            }
        }
    }

    override suspend fun markMessageReceived(
        chatId: String,
        messageId: String,
        authorId: String,
        receivedByUserId: String,
    ): Boolean {
        return withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.transactionWithResult {
                val status = db.messageRecipientsQueries.getOutgoingRecipientStatus(
                    messageId = messageId,
                    receivedByUserId = receivedByUserId,
                    chatId = chatId,
                    authorId = authorId,
                ).executeAsOneOrNull()?.let(::mapStringToStatus)
                    ?: return@transactionWithResult false

                if (status == Status.Created || status == Status.Sent) {
                    db.messageRecipientsQueries.markReceived(
                        messageId = messageId,
                        userId = receivedByUserId,
                    )
                }
                true
            }
        }
    }

    override suspend fun clearAll() {
        withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.transaction {
                db.messageTableQueries.removeAll()
                db.textMessageTableQueries.removeAll()
                db.imageMessageTableQueries.removeAll()
                db.messageRecipientsQueries.removeAll()
                db.messageStatusMigrationQueries.removeAll()
                db.messageReceivedEventsQueries.removeAll()
            }
        }
    }

    override suspend fun migrateMessageStatuses(userId: String) {
        withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            migrationMutex.withLock {
                if (!migrationCompleted) {
                    if (db.messageStatusMigrationQueries.getPendingCount().executeAsOne() > 0) {
                        db.transaction {
                            db.messageStatusMigrationQueries.initializeLocalStatuses(userId)
                            db.messageStatusMigrationQueries.removeAll()
                        }
                    }
                    migrationCompleted = true
                }
            }
        }
    }

    private suspend fun ChatDb.persistMessage(message: MessageSM) {
        messageTableQueries.insert(message.toMessageTable())
        insertMessageDetail(message)
        message.recipients.forEach { recipient ->
            messageRecipientsQueries.insert(
                messageId = message.id,
                userId = recipient.userId,
                status = mapStatusToString(recipient.status),
            )
        }
    }

    private suspend fun ChatDb.insertMessageDetail(message: MessageSM) {
        when (message) {
            is MessageSM.Text -> textMessageTableQueries.insert(message.toTextTable())
            is MessageSM.Image -> imageMessageTableQueries.insert(message.toImageTable())
        }
    }

    private fun ChatDb.attachRecipients(rows: List<JoinedMessageRow>): List<MessageSM> {
        if (rows.isEmpty()) return emptyList()

        val recipientsByMessageId = messageRecipientsQueries
            .getByMessageIds(messageIds = rows.map { it.id })
            .executeAsList()
            .mapNotNull { recipient ->
                val status = mapStringToStatus(recipient.status) ?: return@mapNotNull null
                recipient.messageId to MessageRecipientSM(
                    userId = recipient.userId,
                    status = status,
                )
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })

        return rows.mapNotNull { row ->
            row.toSM(recipients = recipientsByMessageId[row.id].orEmpty())
        }
    }
}
