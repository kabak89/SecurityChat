package com.security.chat.multiplatform.features.chats.data.storage

import com.security.chat.multiplatform.common.core.db.DatabaseCreator
import com.security.chat.multiplatform.common.core.db.SecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chats.data.storage.entity.ChatSM
import com.security.chat.multiplatform.features.chats.data.storage.mapper.toSM
import com.security.chat.multiplatform.features.chats.data.storage.mapper.toTable
import kotlinx.coroutines.withContext

public interface ChatsStorage {
    public suspend fun saveChats(chats: List<ChatSM>)
    public suspend fun getChat(id: String): ChatSM?
    public suspend fun clearAll()
}

internal class ChatsStorageImpl(
    private val dispatcherProvider: DispatcherProviderInterface,
    private val driverFactory: SecuredDatabaseDriverFactory,
) : ChatsStorage {

    private val dbCreator: DatabaseCreator<ChatsDb> =
        DatabaseCreator(
            dispatcherProvider = dispatcherProvider,
            create = {
                ChatsDb(
                    driver = driverFactory.createDriver(
                        databaseName = "personal_chats.db",
                        sqlSchema = ChatsDb.Schema,
                        version = 1,
                    ),
                )
            },
        )

    override suspend fun saveChats(chats: List<ChatSM>) {
        withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.personalChatTableQueries.transaction {
                db.personalChatTableQueries.removeAll()

                chats
                    .map { it.toTable() }
                    .forEach { table ->
                        db.personalChatTableQueries.insert(table)
                    }
            }
        }
    }

    override suspend fun getChat(id: String): ChatSM? {
        return withContext(dispatcherProvider.IO) {
            dbCreator.getDb().personalChatTableQueries.getById(id).executeAsOneOrNull()?.toSM()
        }
    }

    override suspend fun clearAll() {
        withContext(dispatcherProvider.IO) {
            dbCreator.getDb().personalChatTableQueries.removeAll()
        }
    }
}