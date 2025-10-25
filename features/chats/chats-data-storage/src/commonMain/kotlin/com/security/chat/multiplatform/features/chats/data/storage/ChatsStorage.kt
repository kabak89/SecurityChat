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
                        databaseName = "chats.db",
                        sqlSchema = ChatsDb.Schema,
                        version = 1,
                    ),
                )
            },
        )

    override suspend fun saveChats(chats: List<ChatSM>) {
        withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.chatTableQueries.transaction {
                db.chatTableQueries.removeAll()

                chats
                    .map { it.toTable() }
                    .forEach { table ->
                        db.chatTableQueries.insert(table)
                    }
            }
        }
    }

    override suspend fun getChat(id: String): ChatSM? {
        return withContext(dispatcherProvider.IO) {
            dbCreator.getDb().chatTableQueries.getById(id).executeAsOneOrNull()?.toSM()
        }
    }

}