package com.security.chat.multiplatform.features.mesh.data.storage

import app.cash.sqldelight.coroutines.asFlow
import com.security.chat.multiplatform.common.core.db.DatabaseCreator
import com.security.chat.multiplatform.common.core.db.SecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

public class MeshLogStorage(
    private val dispatcherProvider: DispatcherProviderInterface,
    driverFactory: SecuredDatabaseDriverFactory,
) {

    private val dbCreator: DatabaseCreator<MeshLogsDb> by lazy {
        DatabaseCreator(dispatcherProvider) {
            MeshLogsDb(
                driver = driverFactory.createDriver(
                    databaseName = "mesh-logs.db",
                    sqlSchema = MeshLogsDb.Schema,
                    version = 1,
                ),
            )
        }
    }

    public suspend fun insert(recordedAtMillis: Long, message: String) {
        withContext(dispatcherProvider.IO) {
            dbCreator.getDb().meshLogRowQueries.insertLog(recordedAtMillis, message)
        }
    }

    public fun getLogsFlow(): Flow<List<MeshLogRow>> {
        return flow { emitAll(dbCreator.dbFlow) }
            .flatMapLatest { db ->
                db.meshLogRowQueries.getLogs().asFlow().map { query ->
                    withContext(dispatcherProvider.IO) { query.executeAsList() }
                }
            }
            .flowOn(dispatcherProvider.IO)
    }
}
