package com.security.chat.multiplatform.features.chat.data.storage

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.security.chat.multiplatform.common.core.db.SecuredDatabaseDriverFactory
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageRecipientSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ChatStorageMigrationTest {

    @Test
    fun `upgrading version 11 keeps existing history visible`() {
        runTest {
            val file = Files.createTempFile("chat-history", ".db")
            try {
                val legacySchema = Path.of("src/commonMain/sqldelight/databases/11.db")
                Files.copy(legacySchema, file, REPLACE_EXISTING)
                val url = "jdbc:sqlite:$file"
                JdbcSqliteDriver(url).use { driver ->
                    driver.seedVersion11()
                }

                withUpgradedStorage(url) { storage ->
                    val message = assertIs<MessageSM.Text>(
                        storage.getNewestMessages(chatId = "chat", limit = 60).single(),
                    )
                    assertEquals("original", message.id)
                    assertEquals("Existing message", message.text)
                    assertEquals(listOf(MessageRecipientSM("me", Status.Read)), message.recipients)
                    assertEquals(null, message.replyToMessageId)
                }
            } finally {
                Files.deleteIfExists(file)
            }
        }
    }

    private suspend fun TestScope.withUpgradedStorage(
        url: String,
        block: suspend (ChatStorage) -> Unit,
    ) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var openedDriver: SqlDriver? = null
        val storage = ChatStorageImpl(
            dispatcherProvider = object : DispatcherProviderInterface {
                override val Default = dispatcher
                override val IO = dispatcher
                override val Unconfined = dispatcher
                override val Main = Dispatchers.Main
            },
            driverFactory = object : SecuredDatabaseDriverFactory {
                override fun createDriver(
                    databaseName: String,
                    sqlSchema: SqlSchema<QueryResult.AsyncValue<Unit>>,
                    version: Int,
                ): SqlDriver {
                    assertEquals(ChatDb.Schema.version.toInt(), version)
                    return JdbcSqliteDriver(
                        url = url,
                        properties = Properties(),
                        schema = object : SqlSchema<QueryResult.Value<Unit>> {
                            override val version = version.toLong()

                            override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
                                error("The history database already exists")
                            }

                            override fun migrate(
                                driver: SqlDriver,
                                oldVersion: Long,
                                newVersion: Long,
                                vararg callbacks: AfterVersion,
                            ): QueryResult.Value<Unit> {
                                return QueryResult.Value(
                                    runBlocking {
                                        sqlSchema.migrate(
                                            driver = driver,
                                            oldVersion = oldVersion,
                                            newVersion = newVersion,
                                            callbacks = callbacks,
                                        ).await()
                                    },
                                )
                            }
                        },
                    ).also { openedDriver = it }
                }
            },
        )
        try {
            block(storage)
        } finally {
            openedDriver?.close()
        }
    }

    private fun SqlDriver.seedVersion11() {
        val statements = listOf(
            """
                INSERT INTO MessageTable (id, chatId, authorId, timestamp, type)
                VALUES ('original', 'chat', 'peer', 1, 'Text')
            """.trimIndent(),
            "INSERT INTO TextMessageTable VALUES ('original', 'Existing message')",
            "INSERT INTO MessageRecipients VALUES ('original', 'me', 'Read')",
            "PRAGMA user_version = 11",
        )
        statements.forEach { sql -> execute(identifier = null, sql = sql, parameters = 0) }
    }
}
