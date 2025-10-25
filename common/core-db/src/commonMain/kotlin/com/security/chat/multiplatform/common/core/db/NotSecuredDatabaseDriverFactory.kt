package com.security.chat.multiplatform.common.core.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

public interface NotSecuredDatabaseDriverFactory {

    public fun createDriver(
        databaseName: String,
        sqlSchema: SqlSchema<QueryResult.AsyncValue<Unit>>,
        version: Int,
    ): SqlDriver

}