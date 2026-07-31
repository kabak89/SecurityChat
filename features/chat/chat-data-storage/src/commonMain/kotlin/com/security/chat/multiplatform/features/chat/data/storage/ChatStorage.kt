package com.security.chat.multiplatform.features.chat.data.storage

import app.cash.sqldelight.coroutines.asFlow
import com.security.chat.multiplatform.common.core.db.DatabaseCreator
import com.security.chat.multiplatform.common.core.db.SecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chat.data.storage.entity.JoinedMessageRow
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.data.storage.mapper.toImageTable
import com.security.chat.multiplatform.features.chat.data.storage.mapper.toMessageTable
import com.security.chat.multiplatform.features.chat.data.storage.mapper.toSM
import com.security.chat.multiplatform.features.chat.data.storage.mapper.toTextTable
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
                        version = 6,
                    ),
                )
            },
        )

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
                db.messageTableQueries
                    .getPaged(
                        chatId = chatId,
                        limit = 1L,
                        offset = 0L,
                        mapper = ::JoinedMessageRow,
                    )
                    .asFlow()
                    .map { it.executeAsOneOrNull() }
                    .distinctUntilChanged()
                    .map { }
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

    override suspend fun clearAll() {
        withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.transaction {
                db.messageTableQueries.removeAll()
                db.textMessageTableQueries.removeAll()
                db.imageMessageTableQueries.removeAll()
                db.messageRecipientsQueries.removeAll()
            }
        }
    }

    private suspend fun ChatDb.persistMessage(message: MessageSM) {
        messageTableQueries.insert(message.toMessageTable())
        insertMessageDetail(message)
        message.recipients.forEach { recipient ->
            messageRecipientsQueries.insert(
                MessageRecipients(
                    messageId = message.id,
                    userId = recipient,
                ),
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

        val recipientsByMessageId: Map<String, List<String>> = messageRecipientsQueries
            .getUserIdsByMessageIds(messageIds = rows.map { it.id })
            .executeAsList()
            .groupBy(keySelector = { it.messageId }, valueTransform = { it.userId })

        return rows.mapNotNull { row ->
            row.toSM(recipients = recipientsByMessageId[row.id].orEmpty())
        }
    }
}
