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

/**
 * Creates or migrates [schema] on [driver], tracking the applied version via `PRAGMA user_version`.
 * Needed because [javax.sql.DataSource.asJdbcDriver] does not run schema creation/migration on its
 * own (unlike the `JdbcSqliteDriver(url, properties, schema)` constructor). The version bookkeeping
 * mirrors what `JdbcSqliteDriver` does internally, so databases created by the old driver keep their
 * data.
 */
internal fun initSchema(
    driver: SqlDriver,
    schema: SqlSchema<QueryResult.Value<Unit>>,
) {
    val currentVersion = driver.executeQuery(
        identifier = null,
        sql = "PRAGMA user_version",
        parameters = 0,
        mapper = { cursor ->
            QueryResult.Value(
                if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L,
            )
        },
    ).value

    when {
        currentVersion == 0L -> {
            schema.create(driver)
            driver.setUserVersion(schema.version)
        }

        currentVersion < schema.version -> {
            schema.migrate(driver, currentVersion, schema.version)
            driver.setUserVersion(schema.version)
        }
    }
}

private fun SqlDriver.setUserVersion(version: Long) {
    execute(identifier = null, sql = "PRAGMA user_version = $version", parameters = 0)
}
