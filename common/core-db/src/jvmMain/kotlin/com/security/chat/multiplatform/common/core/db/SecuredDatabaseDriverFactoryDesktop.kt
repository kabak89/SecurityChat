package com.security.chat.multiplatform.common.core.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.security.chat.multiplatform.common.settings.EncryptedSettings
import java.util.UUID

internal class SecuredDatabaseDriverFactoryDesktop(
    private val encryptedSettings: EncryptedSettings,
) : SecuredDatabaseDriverFactory {

    override fun createDriver(
        databaseName: String,
        sqlSchema: SqlSchema<QueryResult.AsyncValue<Unit>>,
        version: Int,
    ): SqlDriver {
        val migrationSchema = DestructiveMigrationSchema(
            schema = sqlSchema,
            version = version.toLong(),
        ).synchronous()

        val passwordKey = getDbPasswordKey(databaseName = databaseName)
        val dbPassword = encryptedSettings.getString(passwordKey) ?: run {
            val newPassword = UUID.randomUUID().toString()
            encryptedSettings.putString(key = passwordKey, value = newPassword)
            newPassword
        }

        return JdbcSqliteDriver(
            url = desktopJdbcSqliteUrl(databaseName),
            properties = securedDesktopJdbcProperties(dbPassword),
            schema = migrationSchema,
        )
    }
}