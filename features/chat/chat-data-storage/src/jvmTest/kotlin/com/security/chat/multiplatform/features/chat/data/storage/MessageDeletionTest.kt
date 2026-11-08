package com.security.chat.multiplatform.features.chat.data.storage

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
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessageDeletionTest {
    @Test
    fun `deleting a message removes content recipients receipt queues and unread count together`() =
        runTest {
            withStorage { storage, driver ->
                storage.saveReceivedMessages(messages = listOf(incoming()), receivedByUserId = "me")
                storage.markMessagesReadUpTo(chatId = "chat", messageId = "message", userId = "me")
                val changed = backgroundScope.async {
                    storage.observeMessagesChanges("chat").drop(1).first()
                }
                runCurrent()
                storage.delete()
                changed.await()
                assertNull(storage.getMessage(chatId = "chat", messageId = "message"))
                assertTrue(storage.observePendingReceivedEvents("me").first().isEmpty())
                assertTrue(storage.observePendingReadEvents("me").first().isEmpty())
                assertTrue(storage.observeUnreadCounts("me").first().isEmpty())
                assertTrue(storage.observeLastMessageTimestamps().first().isEmpty())
            }
        }

    @Test
    fun `image deletion returns the file to clean and duplicates are harmless`() =
        runTest {
            withStorage { storage, _ ->
                val image = MessageSM.Image(
                    id = "message",
                    chatId = "chat",
                    recipients = listOf(
                        MessageRecipientSM(userId = "me", status = Status.ReceivedByReceiver),
                    ),
                    authorId = "peer",
                    timestamp = 1,
                    fileId = "image-file",
                    key = "key",
                    isDownloaded = true,
                )
                storage.saveMessage(image)
                assertEquals("image-file", storage.delete())
                assertEquals(null, storage.delete())
                assertTrue(
                    storage.isMessageDeleted(
                        chatId = "chat",
                        messageId = "message",
                        authorId = "peer",
                    ),
                )
                assertTrue(storage.getNewestMessages(chatId = "chat", limit = 10).isEmpty())
            }
        }

    @Test
    fun `another author cannot delete an existing message or suppress it before delivery`() =
        runTest {
            withStorage { storage, _ ->
                storage.delete(deletedByUserId = "attacker")
                storage.saveMessage(incoming())
                storage.delete(deletedByUserId = "attacker")
                assertEquals(
                    "peer",
                    storage.getMessage(chatId = "chat", messageId = "message")?.authorId,
                )
                assertFalse(
                    storage.isMessageDeleted(
                        chatId = "chat",
                        messageId = "message",
                        authorId = "peer",
                    ),
                )
            }
        }

    @Test
    fun `deletion before a message prevents history live push and updates from restoring it`() =
        runTest {
            withStorage { storage, _ ->
                storage.delete()
                storage.saveMessage(incoming())
                storage.saveMessages(listOf(incoming()))
                storage.saveReceivedMessages(messages = listOf(incoming()), receivedByUserId = "me")
                storage.updateMessage(incoming())
                assertTrue(storage.getNewestMessages(chatId = "chat", limit = 10).isEmpty())
                assertTrue(storage.observePendingReceivedEvents("me").first().isEmpty())
                storage.saveMessage(incoming(chatId = "other"))
                assertEquals(1, storage.getNewestMessages(chatId = "other", limit = 10).size)
            }
        }

    @Test
    fun `deletions survive database reopening and clearing storage removes them`() =
        runTest {
            val directory = Files.createTempDirectory("chat-deletions")
            val path = directory.resolve("chat.db")
            val url = "jdbc:sqlite:$path"
            try {
                withStorage(databaseUrl = url) { storage, _ ->
                    storage.saveMessage(incoming())
                    storage.delete()
                }
                withStorage(
                    databaseUrl = url, initializeSchema = false,
                    block = { storage, _ ->
                        storage.saveMessage(incoming())
                        assertTrue(storage.getNewestMessages(chatId = "chat", limit = 10).isEmpty())
                        storage.clearAll()
                        storage.saveMessage(incoming())
                        assertEquals(1, storage.getNewestMessages(chatId = "chat", limit = 10).size)
                    },
                )
            } finally {
                Files.deleteIfExists(path)
                Files.deleteIfExists(directory)
            }
        }

    @Test
    fun `failed deletion rolls back content removal and its tombstone`() =
        runTest {
            withStorage { storage, driver ->
                storage.saveMessage(incoming())
                driver.execute(
                    identifier = null,
                    sql = """
                        CREATE TRIGGER fail_delete BEFORE DELETE ON MessageTable
                        BEGIN SELECT RAISE(ABORT, 'delete failed'); END;
                    """.trimIndent(),
                    parameters = 0,
                ).await()
                assertFailsWith<Exception> {
                    storage.delete()
                }
                val message =
                    storage.getMessage(chatId = "chat", messageId = "message") as MessageSM.Text
                assertEquals("text", message.text)
                assertFalse(
                    storage.isMessageDeleted(
                        chatId = "chat",
                        messageId = "message",
                        authorId = "peer",
                    ),
                )
            }
        }

    @Test
    fun `queued deletion survives reopening and is cleared when the deletion is applied`() =
        runTest {
            val directory = Files.createTempDirectory("pending-chat-deletions")
            val path = directory.resolve("chat.db")
            val url = "jdbc:sqlite:$path"
            try {
                withStorage(databaseUrl = url) { storage, _ ->
                    storage.saveMessage(incoming())
                    assertFalse(
                        storage.enqueueMessageDeletion(
                            chatId = "chat", messageId = "message", authorId = "other",
                        ),
                    )
                    repeat(2) {
                        assertTrue(
                            storage.enqueueMessageDeletion(
                                chatId = "chat", messageId = "message", authorId = "peer",
                            ),
                        )
                    }
                }
                withStorage(
                    databaseUrl = url, initializeSchema = false,
                    block = { storage, driver ->
                        val pending = ChatDb(driver).pendingMessageDeletionsQueries
                        assertEquals(
                            1L,
                            pending.isPending(
                                chatId = "chat", messageId = "message", authorId = "peer",
                            ).executeAsOne(),
                        )
                        assertEquals(
                            "peer",
                            storage.getMessage(chatId = "chat", messageId = "message")?.authorId,
                        )
                        assertFalse(
                            storage.isMessageDeleted(
                                chatId = "chat",
                                messageId = "message",
                                authorId = "peer",
                            ),
                        )
                        storage.delete()
                        assertEquals(
                            0L,
                            pending.isPending(
                                chatId = "chat", messageId = "message", authorId = "peer",
                            ).executeAsOne(),
                        )
                        assertFalse(
                            storage.enqueueMessageDeletion(
                                chatId = "chat", messageId = "message", authorId = "peer",
                            ),
                        )
                    },
                )
            } finally {
                Files.deleteIfExists(path)
                Files.deleteIfExists(directory)
            }
        }

    @Test
    fun `reply id survives arrival and deletion of the original and lookup stays in the chat`() {
        runTest {
            withStorage { storage, _ ->
                val reply = incoming().copy(id = "reply", replyToMessageId = "message")
                storage.saveMessage(reply)
                storage.saveMessage(incoming().copy(id = "foreign", chatId = "other"))
                val targetIds = setOf("message", "foreign")

                assertTrue(
                    storage.getMessagesByIds(chatId = "chat", messageIds = targetIds).isEmpty(),
                )
                storage.saveMessage(incoming())
                assertEquals(
                    listOf("message"),
                    storage.getMessagesByIds(chatId = "chat", messageIds = targetIds).map { it.id },
                )

                storage.delete()
                assertTrue(
                    storage.getMessagesByIds(chatId = "chat", messageIds = targetIds).isEmpty(),
                )
                assertEquals(
                    "message",
                    storage.getMessage(chatId = "chat", messageId = "reply")?.replyToMessageId,
                )
            }
        }
    }

    @Test
    fun `history cursors include messages with the same timestamp on both sides of a quote`() {
        runTest {
            withStorage { storage, _ ->
                storage.saveMessages(listOf("a", "b", "c").map { incoming().copy(id = it) })
                val timestamp = incoming().timestamp

                val older = storage.getMessagesAtOrBefore(
                    chatId = "chat",
                    beforeTimestamp = timestamp,
                    beforeId = "b",
                    limit = 10,
                )
                val newer = storage.getClosestNewerMessages(
                    chatId = "chat",
                    afterTimestamp = timestamp,
                    afterId = "b",
                    limit = 10,
                )
                assertEquals(listOf("b", "a"), older.map { it.id })
                assertEquals(listOf("c"), newer.map { it.id })
            }
        }
    }

    private suspend fun ChatStorage.delete(deletedByUserId: String = "peer"): String? =
        deleteMessage(chatId = "chat", messageId = "message", deletedByUserId = deletedByUserId)

    private suspend fun TestScope.withStorage(
        databaseUrl: String = JdbcSqliteDriver.IN_MEMORY,
        initializeSchema: Boolean = true,
        block: suspend (ChatStorage, SqlDriver) -> Unit,
    ) {
        val driver = JdbcSqliteDriver(databaseUrl)
        try {
            if (initializeSchema) ChatDb.Schema.create(driver).await()
            val dispatcher = StandardTestDispatcher(testScheduler)
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
                    ): SqlDriver = driver
                },
            )
            block(storage, driver)
        } finally {
            driver.close()
        }
    }

    private fun incoming(
        id: String = "message",
        timestamp: Long = 1,
        authorId: String = "peer",
        chatId: String = "chat",
        userId: String = "me",
        status: Status = Status.ReceivedByReceiver,
    ): MessageSM.Text =
        MessageSM.Text(
            id = id,
            chatId = chatId,
            text = "text",
            recipients = listOf(MessageRecipientSM(userId, status)),
            authorId = authorId,
            timestamp = timestamp,
        )

}
