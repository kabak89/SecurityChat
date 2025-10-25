package com.security.chat.multiplatform.common.core.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.wifimap.wifimap.multiplatform.common.core.db.DestructiveMigrationSchema
import kotlinx.coroutines.runBlocking
import java.util.Properties

//TODO in memory DB
internal class NotSecuredDatabaseDriverFactoryDesktop : NotSecuredDatabaseDriverFactory {

    override fun createDriver(
        databaseName: String,
        sqlSchema: SqlSchema<QueryResult.AsyncValue<Unit>>,
        version: Int,
    ): SqlDriver {
        return JdbcSqliteDriver(
            url = JdbcSqliteDriver.IN_MEMORY,
            properties = Properties().apply { put("foreign_keys", "true") },
        )
            .apply {
                runBlocking {
                    DestructiveMigrationSchema(
                        schema = sqlSchema,
                        version = version.toLong(),
                    ).create(this@apply).await()
                }
            }
    }
}