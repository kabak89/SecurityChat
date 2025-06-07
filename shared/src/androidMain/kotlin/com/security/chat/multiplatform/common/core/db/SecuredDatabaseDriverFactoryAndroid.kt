package com.security.chat.multiplatform.common.core.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.security.chat.multiplatform.common.settings.EncryptedSettings
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.util.UUID

internal class SecuredDatabaseDriverFactoryAndroid(
    private val context: Context,
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

        val callback = object : AndroidSqliteDriver.Callback(migrationSchema) {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.setForeignKeyConstraintsEnabled(true)
            }

            override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                onUpgrade(db, oldVersion, newVersion)
            }
        }

        //TODO replace with build type
        val secured = false

        return when (secured) {
            false -> {
                AndroidSqliteDriver(
                    schema = migrationSchema,
                    context = context,
                    name = databaseName,
                    callback = callback,
                    factory = RequerySQLiteOpenHelperFactory(),
                )
            }

            true -> {
                val passwordKey = getDbPasswordKey(databaseName = databaseName)

                val dbPassword = encryptedSettings.getString(passwordKey) ?: run {
                    val newPassword = UUID.randomUUID().toString()
                    encryptedSettings.putString(key = passwordKey, value = newPassword)
                    newPassword
                }

                System.loadLibrary("sqlcipher")

                return AndroidSqliteDriver(
                    schema = migrationSchema,
                    context = context,
                    name = databaseName,
                    callback = callback,
                    factory = SupportOpenHelperFactory(
                        dbPassword.toByteArray(),
                    ),
                )
            }
        }
    }
}

/**
 * Copy of extension from SQLDelight. It appears in build folder after adding
 * "generateAsync = true" in SQLDelight config
 */
internal fun SqlSchema<QueryResult.AsyncValue<Unit>>.synchronous(): SqlSchema<QueryResult.Value<Unit>> =
    object : SqlSchema<QueryResult.Value<Unit>> {
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