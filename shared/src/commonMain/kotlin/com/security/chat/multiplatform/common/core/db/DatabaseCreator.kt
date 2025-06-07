package com.security.chat.multiplatform.common.core.db

import app.cash.sqldelight.SuspendingTransacter
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Class for lazy creation of database. IMPORTANT: create only one instance of [DatabaseCreator] for
 * every database. Otherwise changes between databases will no sync or will be synced after some
 * time
 */
class DatabaseCreator<T : SuspendingTransacter>(
    private val dispatcherProvider: DispatcherProviderInterface,
    private val create: () -> (T),
) {

    private val database: T by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
        create()
    }

    val dbFlow: Flow<T> =
        flowOf(database)
            .flowOn(dispatcherProvider.IO)

    suspend fun getDb(): T {
        return withContext(dispatcherProvider.IO) { database }
    }

}