package com.security.chat.multiplatform.common.core.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.Properties

internal class NotSecuredDatabaseDriverFactoryDesktop : NotSecuredDatabaseDriverFactory {

    override fun createDriver(
        databaseName: String,
        sqlSchema: SqlSchema<QueryResult.AsyncValue<Unit>>,
        version: Int,
    ): SqlDriver {
        val migrationSchema = DestructiveMigrationSchema(
            schema = sqlSchema,
            version = version.toLong(),
        ).synchronous()

        return JdbcSqliteDriver(
            url = desktopJdbcSqliteUrl(databaseName),
            properties = Properties().apply { put("foreign_keys", "true") },
            schema = migrationSchema,
        )
    }
}