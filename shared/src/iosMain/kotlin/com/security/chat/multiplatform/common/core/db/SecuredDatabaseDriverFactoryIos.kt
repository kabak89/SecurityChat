package com.security.chat.multiplatform.common.core.db

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import com.security.chat.multiplatform.common.settings.EncryptedSettings

internal class SecuredDatabaseDriverFactoryIos(
    private val encryptedSettings: EncryptedSettings,
) : SecuredDatabaseDriverFactory {

    override fun createDriver(
        databaseName: String,
        sqlSchema: SqlSchema<QueryResult.AsyncValue<Unit>>,
        version: Int,
    ): SqlDriver {
        val schema = DestructiveMigrationSchema(
            schema = sqlSchema,
            version = version.toLong(),
        )

        //TODO not actually secured

        val onConfiguration = { config: DatabaseConfiguration ->
            config.copy(
                extendedConfig = DatabaseConfiguration.Extended(
                    foreignKeyConstraints = true,
                ),
            )
        }

        return NativeSqliteDriver(
            schema = schema.synchronous(),
            name = databaseName,
            onConfiguration = onConfiguration,
        )
    }
}