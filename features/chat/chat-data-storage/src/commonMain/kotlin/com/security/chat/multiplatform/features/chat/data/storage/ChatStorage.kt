package com.security.chat.multiplatform.features.chat.data.storage

import app.cash.sqldelight.coroutines.asFlow
import com.security.chat.multiplatform.common.core.db.DatabaseCreator
import com.security.chat.multiplatform.common.core.db.SecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.data.storage.mapper.toSM
import com.security.chat.multiplatform.features.chat.data.storage.mapper.toTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

public interface ChatStorage {
    public suspend fun saveMessage(message: MessageSM)
    public suspend fun saveMessages(messages: List<MessageSM>)
    public suspend fun getMessages(chatId: String, limit: Long, offset: Long): List<MessageSM>
    public fun getMessagesFlow(chatId: String, limit: Long): Flow<List<MessageSM>>
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
                        version = 1,
                    ),
                )
            },
        )

    override suspend fun saveMessage(message: MessageSM) {
        withContext(dispatcherProvider.IO) {
            dbCreator.getDb().messageTableQueries.insert(message.toTable())
        }
    }

    override suspend fun saveMessages(messages: List<MessageSM>) {
        withContext(dispatcherProvider.IO) {
            dbCreator.getDb().messageTableQueries.transaction {
                messages.forEach { message ->
                    dbCreator.getDb().messageTableQueries.insert(message.toTable())
                }
            }
        }
    }

    override suspend fun getMessages(chatId: String, limit: Long, offset: Long): List<MessageSM> {
        return withContext(dispatcherProvider.IO) {
            dbCreator.getDb().messageTableQueries
                .getPaged(
                    chatId = chatId,
                    limit = limit,
                    offset = offset,
                )
                .executeAsList()
                .mapNotNull { it.toSM() }
        }
    }

    override fun getMessagesFlow(chatId: String, limit: Long): Flow<List<MessageSM>> {
        return dbCreator.dbFlow.flatMapLatest { db ->
            db.messageTableQueries
                .getPaged(
                    chatId = chatId,
                    limit = limit,
                    offset = 0,
                )
                .asFlow()
                .map { query ->
                    query.executeAsList()
                        .mapNotNull { table -> table.toSM() }
                }
                .flowOn(dispatcherProvider.IO)
        }
    }

    override suspend fun updateMessage(message: MessageSM) {
        withContext(dispatcherProvider.IO) {
            //TODO check
            dbCreator.getDb().messageTableQueries.insert(message.toTable())
        }
    }

    override suspend fun clearAll() {
        withContext(dispatcherProvider.IO) {
            dbCreator.getDb().messageTableQueries.removeAll()
        }
    }
}