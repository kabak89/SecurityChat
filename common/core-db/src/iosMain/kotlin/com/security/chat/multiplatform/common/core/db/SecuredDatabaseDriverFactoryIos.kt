package com.security.chat.multiplatform.common.core.db

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration
import com.security.chat.multiplatform.common.settings.EncryptedSettings
import io.wifimap.wifimap.multiplatform.common.core.db.DestructiveMigrationSchema
import platform.Foundation.NSUUID

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

        val passwordKey = getDbPasswordKey(databaseName = databaseName)
        val dbPassword = encryptedSettings.getString(passwordKey) ?: run {
            val newPassword = NSUUID().UUIDString()
            encryptedSettings.putString(key = passwordKey, value = newPassword)
            newPassword
        }

        return NativeSqliteDriver(
            configuration = DatabaseConfiguration(
                name = databaseName,
                version = schema.version.toInt(),
                create = { connection ->
                    wrapConnection(
                        connection = connection,
                        block = {
                            schema.synchronous().create(it)
                        },
                    )
                },
                upgrade = { connection, oldVersion, newVersion ->
                    wrapConnection(connection) {
                        schema.synchronous().migrate(
                            driver = it,
                            oldVersion = oldVersion.toLong(),
                            newVersion = newVersion.toLong(),
                        )
                    }
                },
                encryptionConfig = DatabaseConfiguration.Encryption(
                    key = dbPassword,
                ),
                extendedConfig = DatabaseConfiguration.Extended(
                    foreignKeyConstraints = true,
                ),
            ),
        )
    }

}