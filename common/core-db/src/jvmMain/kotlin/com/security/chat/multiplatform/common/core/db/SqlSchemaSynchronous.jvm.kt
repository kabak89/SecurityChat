package com.security.chat.multiplatform.common.core.db

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import kotlinx.coroutines.runBlocking

/**
 * Bridges async SQLDelight schemas (`generateAsync = true`) to JVM drivers that expect
 * [QueryResult.Value], e.g. the `JdbcSqliteDriver(url, properties, schema, …)` overload in
 * `JdbcSqliteSchema.kt`.
 */
internal fun SqlSchema<QueryResult.AsyncValue<Unit>>.synchronous(): SqlSchema<QueryResult.Value<Unit>> {
    return object : SqlSchema<QueryResult.Value<Unit>> {
        override val version = this@synchronous.version

        override fun create(driver: SqlDriver) = QueryResult.Value(
            runBlocking {
                this@synchronous.create(driver).await()
            },
        )

        override fun migrate(
            driver: SqlDriver,
            oldVersion: Long,
            newVersion: Long,
            vararg callbacks: AfterVersion,
        ) = QueryResult.Value(
            runBlocking {
                this@synchronous.migrate(driver, oldVersion, newVersion, *callbacks).await()
            },
        )
    }
}
