package com.security.chat.multiplatform.features.chat.data.storage

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.security.chat.multiplatform.common.core.db.DatabaseCreator
import com.security.chat.multiplatform.common.core.db.SecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.chat.data.storage.entity.JoinedMessageRow
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageReadEventSM
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
import kotlinx.coroutines.flow.distinctUntilChanged
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
    public suspend fun enqueueStoredReadEvents(chatId: String, readByUserId: String)
    public fun observePendingReadEvents(readByUserId: String): Flow<List<MessageReadEventSM>>
    public suspend fun markReadEventPublished(id: String, readByUserId: String)
    public suspend fun getMessages(chatId: String, limit: Long, offset: Long): List<MessageSM>
    public suspend fun getNewestMessages(chatId: String, limit: Long): List<MessageSM>
    public suspend fun getUnreadMessages(chatId: String, userId: String): List<MessageSM>
    public suspend fun getMessagesAtOrBefore(
        chatId: String,
        beforeTimestamp: Long,
        beforeId: String,
        limit: Long,
    ): List<MessageSM>

    /**
     * Returns up to [limit] messages after the ([afterTimestamp], [afterId]) cursor, nearest first.
     */
    public suspend fun getClosestNewerMessages(
        chatId: String,
        afterTimestamp: Long,
        afterId: String,
        limit: Long,
    ): List<MessageSM>

    public suspend fun getMessagesByIds(chatId: String, messageIds: Set<String>): List<MessageSM>

    public fun observeMessagesChanges(chatId: String): Flow<Unit>
    public fun observeLastMessageTimestamps(): Flow<Map<String, Long>>
    public fun observeUnreadCounts(userId: String): Flow<Map<String, Long>>
    public suspend fun updateMessage(message: MessageSM)
    public suspend fun markMessageSent(id: String, acceptedRecipientIds: List<String>? = null)
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

    /** Returns true for a matching outgoing recipient, including an already processed read. */
    public suspend fun markMessageRead(
        chatId: String,
        messageId: String,
        authorId: String,
        readByUserId: String,
    ): Boolean

    public suspend fun markMessagesReadUpTo(chatId: String, messageId: String, userId: String)
    public suspend fun clearAll()

    public suspend fun getMessage(chatId: String, messageId: String): MessageSM?
    public suspend fun isMessageDeleted(
        chatId: String,
        messageId: String,
        authorId: String,
    ): Boolean

    /** Records an author-scoped deletion and returns the removed image file ID for cache cleanup. */
    public suspend fun deleteMessage(
        chatId: String,
        messageId: String,
        deletedByUserId: String,
    ): String?

    public suspend fun enqueueMessageDeletion(
        chatId: String,
        messageId: String,
        authorId: String,
    ): Boolean
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
                        version = ChatDb.Schema.version.toInt(),
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

    override suspend fun enqueueStoredReadEvents(chatId: String, readByUserId: String) {
        withContext(dispatcherProvider.IO) {
            dbCreator.getDb().messageReadEventsQueries.enqueueStored(
                eventIdPrefix = "message.read:${readByUserId.length}:$readByUserId:",
                readByUserId = readByUserId,
                chatId = chatId,
            )
        }
    }

    override fun observePendingReadEvents(readByUserId: String): Flow<List<MessageReadEventSM>> {
        return dbCreator.dbFlow
            .flatMapLatest { db ->
                db.messageReadEventsQueries.getPending(readByUserId)
                    .asFlow().mapToList(dispatcherProvider.IO)
            }
            .map { rows ->
                rows.map { row ->
                    MessageReadEventSM(
                        id = row.id,
                        messageId = row.messageId,
                        chatId = row.chatId,
                        recipientUserId = row.recipientUserId,
                        readByUserId = row.readByUserId,
                    )
                }
            }
            .flowOn(dispatcherProvider.IO)
    }

    override suspend fun markReadEventPublished(id: String, readByUserId: String) {
        withContext(dispatcherProvider.IO) {
            dbCreator.getDb().messageReadEventsQueries.markPublished(
                id = id,
                readByUserId = readByUserId,
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

    override suspend fun getUnreadMessages(chatId: String, userId: String): List<MessageSM> {
        return withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.attachRecipients(
                db.messageTableQueries
                    .getUnread(
                        chatId = chatId,
                        userId = userId,
                        mapper = ::JoinedMessageRow,
                    )
                    .executeAsList(),
            )
        }
    }

    override suspend fun getMessagesAtOrBefore(
        chatId: String,
        beforeTimestamp: Long,
        beforeId: String,
        limit: Long,
    ): List<MessageSM> {
        return withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.attachRecipients(
                db.messageTableQueries
                    .getAtOrBefore(
                        chatId = chatId,
                        beforeTimestamp = beforeTimestamp,
                        beforeId = beforeId,
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
        afterId: String,
        limit: Long,
    ): List<MessageSM> {
        return withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            val joinedMessageRows = db.messageTableQueries
                .getClosestNewerThan(
                    chatId = chatId,
                    afterTimestamp = afterTimestamp,
                    afterId = afterId,
                    limit = limit,
                    mapper = ::JoinedMessageRow,
                )
                .executeAsList()

            db.attachRecipients(
                rows = joinedMessageRows,
            )
        }
    }

    override fun observeLastMessageTimestamps(): Flow<Map<String, Long>> {
        return dbCreator.dbFlow
            .flatMapLatest { db ->
                db.messageTableQueries.getLastMessageTimestamps()
                    .asFlow()
                    .mapToList(dispatcherProvider.IO)
            }
            .map { rows ->
                rows.mapNotNull { row ->
                    row.lastMessageTimestamp?.let { timestamp -> row.chatId to timestamp }
                }.toMap()
            }
            .distinctUntilChanged()
            .flowOn(dispatcherProvider.IO)
    }

    override fun observeUnreadCounts(userId: String): Flow<Map<String, Long>> {
        return dbCreator.dbFlow
            .flatMapLatest { db ->
                db.messageTableQueries.getUnreadCounts(userId)
                    .asFlow()
                    .mapToList(dispatcherProvider.IO)
            }
            .map { rows -> rows.associate { it.chatId to it.unreadCount } }
            .distinctUntilChanged()
            .flowOn(dispatcherProvider.IO)
    }

    override suspend fun getMessagesByIds(
        chatId: String,
        messageIds: Set<String>,
    ): List<MessageSM> {
        return withContext(dispatcherProvider.IO) {
            if (messageIds.isEmpty()) return@withContext emptyList()
            dbCreator.getDb().messageTableQueries
                .getMessagesByIds(
                    chatId = chatId,
                    messageIds = messageIds,
                    mapper = ::JoinedMessageRow,
                )
                .executeAsList()
                .mapNotNull { it.toSM(recipients = emptyList()) }
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
                if (db.deletedMessagesQueries.isDeleted(
                        messageId = message.id,
                        chatId = message.chatId,
                        authorId = message.authorId,
                    ).executeAsOne() == 0L
                ) {
                    db.messageTableQueries.insert(message.toMessageTable())
                    db.insertMessageDetail(message)
                }
            }
        }
    }

    override suspend fun markMessageSent(id: String, acceptedRecipientIds: List<String>?) {
        withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.transaction {
                if (acceptedRecipientIds != null) {
                    db.messageRecipientsQueries.removeUnaccepted(
                        messageId = id,
                        acceptedRecipientIds = acceptedRecipientIds,
                    )
                }
                db.messageRecipientsQueries.markSent(messageId = id)
            }
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
                    db.messageReadEventsQueries.enqueueUpTo(
                        eventIdPrefix = "message.read:${userId.length}:$userId:",
                        readByUserId = userId,
                        messageId = messageId,
                        chatId = chatId,
                    )
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
                db.messageReadEventsQueries.removeAll()
                db.deletedMessagesQueries.removeAll()
                db.pendingMessageDeletionsQueries.removeAll()
            }
        }
    }

    override suspend fun markMessageRead(
        chatId: String,
        messageId: String,
        authorId: String,
        readByUserId: String,
    ): Boolean {
        return withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.transactionWithResult {
                val status = db.messageRecipientsQueries.getOutgoingRecipientStatus(
                    messageId = messageId,
                    receivedByUserId = readByUserId,
                    chatId = chatId,
                    authorId = authorId,
                ).executeAsOneOrNull()?.let(::mapStringToStatus)
                    ?: return@transactionWithResult false

                if (status != Status.Read) {
                    db.messageRecipientsQueries.markRead(
                        messageId = messageId,
                        userId = readByUserId,
                    )
                }
                true
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

    override suspend fun getMessage(chatId: String, messageId: String): MessageSM? {
        return withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.attachRecipients(
                db.messageTableQueries.getMessage(
                    messageId = messageId,
                    chatId = chatId,
                    mapper = ::JoinedMessageRow,
                ).executeAsList(),
            ).singleOrNull()
        }
    }

    override suspend fun isMessageDeleted(
        chatId: String,
        messageId: String,
        authorId: String,
    ): Boolean {
        return withContext(dispatcherProvider.IO) {
            dbCreator.getDb().deletedMessagesQueries.isDeleted(
                messageId = messageId,
                chatId = chatId,
                authorId = authorId,
            ).executeAsOne() > 0
        }
    }

    override suspend fun deleteMessage(
        chatId: String,
        messageId: String,
        deletedByUserId: String,
    ): String? {
        return withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.transactionWithResult {
                val message = db.messageTableQueries.getMessage(
                    messageId = messageId,
                    chatId = chatId,
                    mapper = ::JoinedMessageRow,
                ).executeAsOneOrNull()
                if (message != null && message.authorId != deletedByUserId) {
                    Log.e("Skipped deletion by another author: messageId=$messageId")
                    return@transactionWithResult null
                }
                db.deletedMessagesQueries.insert(
                    messageId = messageId,
                    chatId = chatId,
                    deletedByUserId = deletedByUserId,
                )
                db.pendingMessageDeletionsQueries.remove(
                    chatId = chatId,
                    messageId = messageId,
                    authorId = deletedByUserId,
                )
                if (message == null) return@transactionWithResult null
                db.messageReceivedEventsQueries.removeByMessageId(
                    messageId = messageId,
                    chatId = chatId,
                )
                db.messageReadEventsQueries.removeByMessageId(
                    messageId = messageId,
                    chatId = chatId,
                )
                db.messageRecipientsQueries.removeById(messageId)
                db.messageStatusMigrationQueries.removeById(messageId)
                db.textMessageTableQueries.removeById(messageId)
                db.imageMessageTableQueries.removeById(messageId)
                db.messageTableQueries.removeById(messageId)
                message.fileId
            }
        }
    }

    override suspend fun enqueueMessageDeletion(
        chatId: String,
        messageId: String,
        authorId: String,
    ): Boolean {
        return withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.transactionWithResult {
                val queries = db.pendingMessageDeletionsQueries
                queries.enqueue(chatId = chatId, messageId = messageId, authorId = authorId)
                queries.isPending(chatId = chatId, messageId = messageId, authorId = authorId)
                    .executeAsOne() > 0L
            }
        }
    }

    private suspend fun ChatDb.persistMessage(message: MessageSM) {
        if (deletedMessagesQueries.isDeleted(
                messageId = message.id,
                chatId = message.chatId,
                authorId = message.authorId,
            ).executeAsOne() > 0
        ) {
            return
        }
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
