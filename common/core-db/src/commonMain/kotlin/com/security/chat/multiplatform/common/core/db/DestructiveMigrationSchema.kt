package com.security.chat.multiplatform.common.core.db

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.security.chat.multiplatform.common.log.Log

/**
 * Schema for destructive migrations. When new version of schema differs from old version, all data
 * removes from DB
 */
public class DestructiveMigrationSchema(
    private val schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
    override val version: Long,
) : SqlSchema<QueryResult.AsyncValue<Unit>> by schema {

    override fun migrate(
        driver: SqlDriver,
        oldVersion: Long,
        newVersion: Long,
        vararg callbacks: AfterVersion,
    ): QueryResult.AsyncValue<Unit> {
        println("Migration started, oldVersion = $oldVersion, newVersion = $newVersion")

        Log.d { "Try to use migrations" }

        try {
            return schema.migrate(
                driver = driver,
                oldVersion = oldVersion,
                newVersion = newVersion,
            )
        } catch (e: Exception) {
            Log.e(e, "Migration failed")
        }

        Log.d { "Process destructive migration" }

        /**
         * Tables in BD bounded by relations. So library trying to delete tables with several
         * attempts
         */
        repeat(DELETE_TABLE_REPEAT_COUNT) {
            val tables = driver.executeQuery(
                identifier = null,
                sql = "SELECT name FROM sqlite_master WHERE type='table';",
                parameters = 0,
                mapper = { cursor ->
                    QueryResult.Value(
                        buildList {
                            while (cursor.next().value) {
                                val name = cursor.getString(0)!!
                                if (name != "sqlite_sequence" && name != "android_metadata") {
                                    add(name)
                                }
                            }
                        },
                    )
                },
            ).value

            var errorOccurred = false

            for (table in tables) {
                try {
                    driver.execute(identifier = null, sql = "DROP TABLE $table", parameters = 0)
                } catch (e: Exception) {
                    println("Handled migration error: $e")
                    errorOccurred = true
                }
            }

            if (errorOccurred) {
                return@repeat
            }
        }

        Log.d { "Migration finished" }

        return create(driver)
    }
}

private const val DELETE_TABLE_REPEAT_COUNT = 100