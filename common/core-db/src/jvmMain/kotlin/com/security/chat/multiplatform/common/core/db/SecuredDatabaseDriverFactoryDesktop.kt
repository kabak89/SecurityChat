package com.security.chat.multiplatform.common.core.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import com.security.chat.multiplatform.common.settings.EncryptedSettings
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
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

        /**
         * Single long-lived connection (pool size 1). SQLCipher derives the encryption key
         * (expensive PBKDF2) on every new connection, so reusing one connection keeps the key
         * derivation to a single time instead of per query.
         */
        val config = HikariConfig().apply {
            jdbcUrl = desktopJdbcSqliteUrl(databaseName)
            driverClassName = "org.sqlite.JDBC"
            dataSourceProperties = securedDesktopJdbcProperties(dbPassword)
            maximumPoolSize = 1
            minimumIdle = 1
            maxLifetime = 0
            idleTimeout = 0
            keepaliveTime = 0
            connectionTimeout = CONNECTION_TIMEOUT_MS
            poolName = "sqlcipher-$databaseName"
        }

        val driver = ListeningSqlDriver(HikariDataSource(config).asJdbcDriver())
        initSchema(driver = driver, schema = migrationSchema)
        return driver
    }
}

private const val CONNECTION_TIMEOUT_MS = 10_000L
