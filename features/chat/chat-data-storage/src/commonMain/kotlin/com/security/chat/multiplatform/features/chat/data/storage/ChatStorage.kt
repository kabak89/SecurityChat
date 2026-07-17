package com.security.chat.multiplatform.features.chat.data.storage

import app.cash.sqldelight.coroutines.asFlow
import com.security.chat.multiplatform.common.core.db.DatabaseCreator
import com.security.chat.multiplatform.common.core.db.SecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.data.storage.mapper.toSM
import com.security.chat.multiplatform.features.chat.data.storage.mapper.toTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

public interface ChatStorage {
    public suspend fun saveMessage(message: MessageSM)
    public suspend fun saveMessages(messages: List<MessageSM>)
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
    public suspend fun clearAll()
    public suspend fun getMessageByTimestamp(timestamp: Long): MessageSM?
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
                        version = 3,
                    ),
                )
            },
        )

    override suspend fun saveMessage(message: MessageSM) {
        withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.transaction {
                db.textMessageTableQueries.insert(message.toTable())
                message.recipients.forEach { recipient ->
                    val messageRecipients = MessageRecipients(
                        messageId = message.id,
                        userId = recipient,
                    )
                    db.messageRecipientsQueries.insert(messageRecipients)
                }
            }
        }
    }

    override suspend fun saveMessages(messages: List<MessageSM>) {
        withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.transaction {
                messages.forEach { message ->
                    db.textMessageTableQueries.insert(message.toTable())
                    message.recipients.forEach { recipient ->
                        db.messageRecipientsQueries.insert(
                            MessageRecipients(
                                messageId = message.id,
                                userId = recipient,
                            ),
                        )
                    }
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
            db.textMessageTableQueries
                .getPaged(
                    chatId = chatId,
                    limit = limit,
                    offset = offset,
                )
                .executeAsList()
                .mapNotNull { messageTable ->
                    val recipients = db.messageRecipientsQueries
                        .getUserIdsByMessageId(messageTable.id)
                        .executeAsList()

                    messageTable.toSM(recipients = recipients)
                }
        }
    }

    override suspend fun getNewestMessages(chatId: String, limit: Long): List<MessageSM> {
        return withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.textMessageTableQueries
                .getNewest(
                    chatId = chatId,
                    limit = limit,
                )
                .executeAsList()
                .mapNotNull { messageTable ->
                    val recipients = db.messageRecipientsQueries
                        .getUserIdsByMessageId(messageTable.id)
                        .executeAsList()

                    messageTable.toSM(recipients = recipients)
                }
        }
    }

    override suspend fun getOlderMessages(
        chatId: String,
        beforeTimestamp: Long,
        limit: Long,
    ): List<MessageSM> {
        return withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.textMessageTableQueries
                .getOlderThan(
                    chatId = chatId,
                    beforeTimestamp = beforeTimestamp,
                    limit = limit,
                )
                .executeAsList()
                .mapNotNull { messageTable ->
                    val recipients = db.messageRecipientsQueries
                        .getUserIdsByMessageId(messageTable.id)
                        .executeAsList()

                    messageTable.toSM(recipients = recipients)
                }
        }
    }

    override suspend fun getClosestNewerMessages(
        chatId: String,
        afterTimestamp: Long,
        limit: Long,
    ): List<MessageSM> {
        return withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.textMessageTableQueries
                .getClosestNewerThan(
                    chatId = chatId,
                    afterTimestamp = afterTimestamp,
                    limit = limit,
                )
                .executeAsList()
                .mapNotNull { messageTable ->
                    val recipients = db.messageRecipientsQueries
                        .getUserIdsByMessageId(messageTable.id)
                        .executeAsList()

                    messageTable.toSM(recipients = recipients)
                }
        }
    }

    override fun observeMessagesChanges(chatId: String): Flow<Unit> {
        return dbCreator.dbFlow
            .flatMapLatest { db ->
                db.textMessageTableQueries
                    .getPaged(chatId = chatId, limit = 1L, offset = 0L)
                    .asFlow()
                    .map { it.executeAsOneOrNull() }
                    .distinctUntilChanged()
                    .map { }
            }
            .flowOn(dispatcherProvider.IO)
    }

    override suspend fun updateMessage(message: MessageSM) {
        withContext(dispatcherProvider.IO) {
            dbCreator.getDb().textMessageTableQueries.insert(message.toTable())
        }
    }

    override suspend fun clearAll() {
        withContext(dispatcherProvider.IO) {
            dbCreator.getDb().textMessageTableQueries.removeAll()
            dbCreator.getDb().messageRecipientsQueries.removeAll()
        }
    }

    override suspend fun getMessageByTimestamp(timestamp: Long): MessageSM? {
        return withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.textMessageTableQueries
                .getByTimestamp(timestamp)
                .executeAsList()
                .firstOrNull()
                ?.let { messageTable ->
                    val recipients = db.messageRecipientsQueries
                        .getUserIdsByMessageId(messageTable.id)
                        .executeAsList()

                    messageTable.toSM(recipients = recipients)
                }
        }
    }
}
