package com.security.chat.multiplatform.common.core.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory

internal class NotSecuredDatabaseDriverFactoryAndroid(
    private val context: Context,
) : NotSecuredDatabaseDriverFactory {

    override fun createDriver(
        databaseName: String,
        sqlSchema: SqlSchema<QueryResult.AsyncValue<Unit>>,
        version: Int,
    ): SqlDriver {
        val migrationSchema = DestructiveMigrationSchema(
            schema = sqlSchema,
            version = version.toLong(),
        ).synchronous()

        val callback = object : AndroidSqliteDriver.Callback(migrationSchema) {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.setForeignKeyConstraintsEnabled(true)
            }

            override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                onUpgrade(db, oldVersion, newVersion)
            }
        }

        return AndroidSqliteDriver(
            schema = migrationSchema,
            context = context,
            name = databaseName,
            callback = callback,
            factory = RequerySQLiteOpenHelperFactory(),
        )
    }
}