package com.security.chat.multiplatform.common.core.db

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.SqlDriver

/**
 * Adds query-listener support (SQLDelight reactivity, i.e. `.asFlow()`) on top of a [SqlDriver]
 * obtained from [javax.sql.DataSource.asJdbcDriver], whose `addListener`/`removeListener`/
 * `notifyListeners` are no-ops. Everything except the listener registry is delegated to [delegate].
 *
 * The registry mirrors the one in `JdbcSqliteDriver`, which is process-local: valid here because a
 * single [delegate] (backed by one connection pool) serves the whole application.
 */
internal class ListeningSqlDriver(
    private val delegate: SqlDriver,
) : SqlDriver by delegate {

    private val listeners = linkedMapOf<String, MutableSet<Query.Listener>>()

    override fun addListener(vararg queryKeys: String, listener: Query.Listener) {
        synchronized(listeners) {
            queryKeys.forEach { key ->
                listeners.getOrPut(key) { linkedSetOf() }.add(listener)
            }
        }
    }

    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) {
        synchronized(listeners) {
            queryKeys.forEach { key ->
                listeners[key]?.remove(listener)
            }
        }
    }

    override fun notifyListeners(vararg queryKeys: String) {
        val listenersToNotify = linkedSetOf<Query.Listener>()
        synchronized(listeners) {
            queryKeys.forEach { key ->
                listeners[key]?.let(listenersToNotify::addAll)
            }
        }
        listenersToNotify.forEach(Query.Listener::queryResultsChanged)
    }
}
