package com.security.chat.multiplatform.features.chats.data.storage

import app.cash.sqldelight.coroutines.asFlow
import com.security.chat.multiplatform.common.core.db.DatabaseCreator
import com.security.chat.multiplatform.common.core.db.SecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chats.data.storage.entity.ChatSM
import com.security.chat.multiplatform.features.chats.data.storage.mapper.toSM
import com.security.chat.multiplatform.features.chats.data.storage.mapper.toTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

public interface ChatsStorage {
    public suspend fun saveChats(chats: List<ChatSM>)
    public suspend fun getPersonalChat(id: String): ChatSM.PersonalChat?
    public suspend fun getGroupChat(id: String): ChatSM.GroupChat?
    public fun getChatsFlow(): Flow<List<ChatSM>>
    public fun getPersonalChatFlow(id: String): Flow<ChatSM.PersonalChat?>
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
                        version = 3,
                    ),
                )
            },
        )

    override suspend fun saveChats(chats: List<ChatSM>) {
        withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            db.transaction {
                db.personalChatTableQueries.removeAll()
                db.groupChatTableQueries.removeAll()

                chats
                    .filterIsInstance<ChatSM.PersonalChat>()
                    .map { it.toTable() }
                    .forEach { table ->
                        db.personalChatTableQueries.insert(table)
                    }

                chats
                    .filterIsInstance<ChatSM.GroupChat>()
                    .forEach { chat ->
                        val table = chat.toTable()
                        db.groupChatTableQueries.insert(table)

                        chat.members
                            .forEach { userId ->
                                val groupChatMemberTables = GroupChatMemberTable(
                                    groupChatId = chat.id,
                                    userId = userId,
                                )

                                db.groupChatMemberTableQueries.insert(groupChatMemberTables)
                            }
                    }
            }
        }
    }

    override suspend fun getPersonalChat(id: String): ChatSM.PersonalChat? {
        return withContext(dispatcherProvider.IO) {
            dbCreator.getDb().personalChatTableQueries.getById(id).executeAsOneOrNull()?.toSM()
        }
    }

    override suspend fun getGroupChat(id: String): ChatSM.GroupChat? {
        return withContext(dispatcherProvider.IO) {
            val db = dbCreator.getDb()
            val groupChatTable =
                db.groupChatTableQueries.getById(id).executeAsOneOrNull() ?: return@withContext null

            val members = db.groupChatMemberTableQueries.getUserIdsByGroupChatId(groupChatTable.id)
                .executeAsList()

            ChatSM.GroupChat(
                id = groupChatTable.id,
                members = members,
                authorId = groupChatTable.authorId,
            )
        }
    }

    override suspend fun clearAll() {
        withContext(dispatcherProvider.IO) {
            dbCreator.getDb().personalChatTableQueries.removeAll()
            dbCreator.getDb().groupChatMemberTableQueries.removeAll()
            dbCreator.getDb().groupChatTableQueries.removeAll()
        }
    }

    override fun getChatsFlow(): Flow<List<ChatSM>> {
        return dbCreator.dbFlow
            .flatMapLatest { db ->
                val personalChatsFlow = db.personalChatTableQueries.getAll()
                    .asFlow()
                    .map { query ->
                        query.executeAsList()
                            .map { table -> table.toSM() }
                    }
                    .flowOn(dispatcherProvider.IO)

                val groupChatsFlow = db.groupChatTableQueries.getAll()
                    .asFlow()
                    .map { it.executeAsList() }
                    .flatMapLatest { groupChatIds ->
                        if (groupChatIds.isEmpty()) {
                            flowOf(emptyList())
                        } else {
                            val chatFlows: List<Flow<ChatSM.GroupChat>> =
                                groupChatIds.map { groupChatTable ->
                                    db.groupChatMemberTableQueries
                                        .getUserIdsByGroupChatId(groupChatTable.id)
                                        .asFlow()
                                        .map { membersQuery ->
                                            ChatSM.GroupChat(
                                                id = groupChatTable.id,
                                                authorId = groupChatTable.authorId,
                                                members = membersQuery.executeAsList(),
                                            )
                                        }
                                }
                            combine(chatFlows) { it.toList() }
                        }
                    }
                    .flowOn(dispatcherProvider.IO)

                combine(
                    personalChatsFlow,
                    groupChatsFlow,
                ) { personalChats, groupChats ->
                    personalChats + groupChats
                }
            }
    }

    override fun getPersonalChatFlow(id: String): Flow<ChatSM.PersonalChat?> {
        return dbCreator.dbFlow
            .flatMapLatest { db ->
                db.personalChatTableQueries.getById(id)
                    .asFlow()
                    .map { query ->
                        query.executeAsOneOrNull()?.toSM()
                    }
                    .flowOn(dispatcherProvider.IO)
            }
    }
}