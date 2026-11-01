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
import kotlin.test.assertTrue

class MessageReadStatusTest {
    @Test
    fun `clearing storage removes pending and published delivery and read events`() =
        runTest {
            withStorage { storage, _ ->
                val messages =
                    listOf(incoming(id = "first"), incoming(id = "second", timestamp = 2))
                storage.saveReceivedMessages(messages = messages, receivedByUserId = "me")
                storage.markMessagesReadUpTo(chatId = "chat", messageId = "second", userId = "me")
                val received = storage.observePendingReceivedEvents("me").first().first()
                val read = storage.observePendingReadEvents("me").first().first()
                storage.markReceivedEventPublished(id = received.id, receivedByUserId = "me")
                storage.markReadEventPublished(id = read.id, readByUserId = "me")

                storage.clearAll()
                assertTrue(storage.observePendingReceivedEvents("me").first().isEmpty())
                assertTrue(storage.observePendingReadEvents("me").first().isEmpty())
                assertTrue(storage.getNewestMessages(chatId = "chat", limit = 10).isEmpty())

                storage.saveReceivedMessages(messages = messages, receivedByUserId = "me")
                storage.markMessagesReadUpTo(chatId = "chat", messageId = "second", userId = "me")
                assertEquals(2, storage.observePendingReceivedEvents("me").first().size)
                assertEquals(2, storage.observePendingReadEvents("me").first().size)
            }
        }

    @Test
    fun `pending read receipts survive closing and reopening the database`() =
        runTest {
            val directory = Files.createTempDirectory("chat-read-status")
            val databasePath = directory.resolve("chat.db")
            val databaseUrl = "jdbc:sqlite:$databasePath"
            try {
                withStorage(databaseUrl = databaseUrl) { storage, _ ->
                    storage.saveMessage(incoming())
                    storage.markMessagesReadUpTo(
                        chatId = "chat",
                        messageId = "message",
                        userId = "me",
                    )
                }
                withStorage(
                    databaseUrl = databaseUrl,
                    initializeSchema = false,
                    block = { storage, _ ->
                        val event = storage.observePendingReadEvents("me").first().single()
                        assertEquals("message.read:2:me:message", event.id)
                        assertTrue(
                            storage.getUnreadMessages(chatId = "chat", userId = "me").isEmpty(),
                        )
                        storage.markReadEventPublished(id = event.id, readByUserId = "me")
                    },
                )
                withStorage(
                    databaseUrl = databaseUrl,
                    initializeSchema = false,
                    block = { storage, _ ->
                        storage.enqueueStoredReadEvents(chatId = "chat", readByUserId = "me")
                        assertTrue(storage.observePendingReadEvents("me").first().isEmpty())
                    },
                )
            } finally {
                Files.deleteIfExists(databasePath)
                Files.deleteIfExists(directory)
            }
        }

    @Test
    fun `read receipts for different users have independent ids and publication state`() =
        runTest {
            withStorage { storage, _ ->
                storage.saveMessage(
                    incoming().copy(
                        recipients = listOf("me", "other").map {
                            MessageRecipientSM(it, Status.ReceivedByReceiver)
                        },
                    ),
                )
                for (userId in listOf("me", "other")) {
                    storage.markMessagesReadUpTo(
                        chatId = "chat",
                        messageId = "message",
                        userId = userId,
                    )
                }
                val mine = storage.observePendingReadEvents("me").first().single()
                val theirs = storage.observePendingReadEvents("other").first().single()
                assertEquals("message.read:2:me:message", mine.id)
                assertEquals("message.read:5:other:message", theirs.id)
                storage.markReadEventPublished(id = mine.id, readByUserId = "me")
                assertTrue(storage.observePendingReadEvents("me").first().isEmpty())
                assertEquals(listOf(theirs), storage.observePendingReadEvents("other").first())
            }
        }

    @Test
    fun `reading queues receipts for incoming messages up to the boundary in order`() =
        runTest {
            withStorage { storage, _ ->
                storage.saveMessages(
                    listOf(
                        incoming(id = "b", timestamp = 2),
                        incoming(id = "a", timestamp = 2, authorId = "another-peer"),
                        incoming(id = "first", timestamp = 1),
                        incoming(id = "later", timestamp = 3),
                        incoming(id = "other-chat", timestamp = 1, chatId = "other"),
                        incoming(id = "outgoing", timestamp = 1, authorId = "me"),
                        incoming(id = "other-user", timestamp = 1, userId = "other"),
                    ),
                )

                storage.markMessagesReadUpTo(chatId = "chat", messageId = "a", userId = "me")

                val events = storage.observePendingReadEvents("me").first()
                assertEquals(listOf("first", "a"), events.map { it.messageId })
                assertEquals(listOf("peer", "another-peer"), events.map { it.recipientUserId })
                assertTrue(events.all { it.chatId == "chat" && it.readByUserId == "me" })
                assertEquals(
                    listOf("message.read:2:me:first", "message.read:2:me:a"),
                    events.map { it.id },
                )
                assertEquals(
                    listOf("b", "later"),
                    storage.getUnreadMessages(chatId = "chat", userId = "me").map { it.id },
                )
            }
        }

    @Test
    fun `invalid boundary leaves statuses and the read queue unchanged`() =
        runTest {
            withStorage { storage, _ ->
                storage.saveMessages(
                    listOf(incoming(), incoming(id = "other-chat", chatId = "other")),
                )
                for (boundary in listOf("missing", "other-chat")) {
                    storage.markMessagesReadUpTo(
                        chatId = "chat",
                        messageId = boundary,
                        userId = "me",
                    )
                }
                assertTrue(storage.observePendingReadEvents("me").first().isEmpty())
                assertEquals(1, storage.getUnreadMessages(chatId = "chat", userId = "me").size)
            }
        }

    @Test
    fun `read status and queued receipts roll back together on a database failure`() =
        runTest {
            withStorage { storage, driver ->
                storage.saveMessage(incoming())
                driver.execute(
                    identifier = null,
                    sql = """
                        CREATE TRIGGER fail_read BEFORE UPDATE ON MessageRecipients
                        WHEN NEW.status = 'Read'
                        BEGIN SELECT RAISE(ABORT, 'read failed'); END;
                    """.trimIndent(),
                    parameters = 0,
                ).await()

                assertFailsWith<Exception> {
                    storage.markMessagesReadUpTo(
                        chatId = "chat",
                        messageId = "message",
                        userId = "me",
                    )
                }
                assertTrue(storage.observePendingReadEvents("me").first().isEmpty())
                assertEquals(1, storage.getUnreadMessages(chatId = "chat", userId = "me").size)
            }
        }

    @Test
    fun `repeated reading redelivery and restoration never requeue published receipts`() =
        runTest {
            withStorage { storage, _ ->
                storage.saveMessage(incoming())
                storage.markMessagesReadUpTo(
                    chatId = "chat",
                    messageId = "message",
                    userId = "me",
                )
                val event = storage.observePendingReadEvents("me").first().single()
                storage.markReadEventPublished(id = event.id, readByUserId = "other")
                assertEquals(listOf(event), storage.observePendingReadEvents("me").first())
                storage.markReadEventPublished(id = event.id, readByUserId = "me")

                storage.saveReceivedMessages(messages = listOf(incoming()), receivedByUserId = "me")
                storage.markMessagesReadUpTo(
                    chatId = "chat",
                    messageId = "message",
                    userId = "me",
                )
                storage.enqueueStoredReadEvents(chatId = "chat", readByUserId = "me")

                assertTrue(storage.observePendingReadEvents("me").first().isEmpty())
                assertTrue(storage.getUnreadMessages(chatId = "chat", userId = "me").isEmpty())
            }
        }

    @Test
    fun `restoration queues only this users previously read incoming messages`() =
        runTest {
            withStorage { storage, _ ->
                storage.saveMessages(
                    listOf(
                        incoming(id = "read", status = Status.Read),
                        incoming(id = "unread"),
                        incoming(id = "other-user", userId = "other", status = Status.Read),
                        incoming(id = "outgoing", authorId = "me", status = Status.Read),
                        incoming(id = "other-chat", chatId = "other", status = Status.Read),
                    ),
                )
                repeat(2) {
                    storage.enqueueStoredReadEvents(chatId = "chat", readByUserId = "me")
                }
                assertEquals(
                    listOf("read"),
                    storage.observePendingReadEvents("me").first().map { it.messageId },
                )
                assertTrue(storage.observePendingReadEvents("other").first().isEmpty())
                storage.clearAll()
                assertTrue(storage.observePendingReadEvents("me").first().isEmpty())
            }
        }

    @Test
    fun `reading queues images and messages beyond one page using an outgoing boundary`() =
        runTest {
            withStorage { storage, _ ->
                val messages = (1..150).map { incoming(id = "text-$it", timestamp = it.toLong()) }
                val image = MessageSM.Image(
                    id = "image",
                    chatId = "chat",
                    recipients = listOf(MessageRecipientSM("me", Status.ReceivedByReceiver)),
                    authorId = "peer",
                    timestamp = 151,
                    fileId = "file",
                    key = "key",
                    isDownloaded = true,
                )
                storage.saveMessages(
                    messages + image + incoming(id = "boundary", timestamp = 152, authorId = "me"),
                )
                storage.markMessagesReadUpTo(
                    chatId = "chat",
                    messageId = "boundary",
                    userId = "me",
                )
                assertEquals(
                    (messages + image).map { it.id },
                    storage.observePendingReadEvents("me").first().map { it.messageId },
                )
            }
        }

    @Test
    fun `read events update only the matching recipient and notify message observers`() =
        runTest {
            withStorage { storage, _ ->
                storage.saveMessage(outgoing(Status.Sent))
                val changed = backgroundScope.async {
                    storage.observeMessagesChanges("chat").drop(1).first()
                }
                runCurrent()
                assertTrue(storage.read())
                changed.await()
                val recipients = storage.getNewestMessages(chatId = "chat", limit = 1)
                    .single().recipients.associate { it.userId to it.status }
                assertEquals(Status.Read, recipients["peer"])
                assertEquals(Status.Sent, recipients["other"])
                assertEquals(Status.Sent, recipients["me"])
            }
        }

    @Test
    fun `read before delivery duplicate events and redelivery always preserve read status`() =
        runTest {
            withStorage { storage, _ ->
                for (status in Status.entries) {
                    storage.clearAll()
                    storage.saveMessage(outgoing(status))
                    repeat(2) { assertTrue(storage.read()) }
                    assertTrue(
                        storage.markMessageReceived(
                            chatId = "chat",
                            messageId = "message",
                            authorId = "me",
                            receivedByUserId = "peer",
                        ),
                    )
                    storage.markMessageSent("message")
                    storage.saveMessage(outgoing(Status.Sent))
                    assertEquals(
                        Status.Read,
                        storage.getNewestMessages(chatId = "chat", limit = 1).single()
                            .recipients.single { it.userId == "peer" }.status,
                    )
                }
            }
        }

    @Test
    fun `read event rejects another author chat recipient and missing messages`() =
        runTest {
            withStorage { storage, _ ->
                storage.saveMessage(outgoing(Status.Sent))
                assertFalse(storage.read(authorId = "other"))
                assertFalse(storage.read(chatId = "other"))
                assertFalse(storage.read(readByUserId = "unknown"))
                assertFalse(storage.read(readByUserId = "me"))
                assertFalse(storage.read(messageId = "missing"))
                assertEquals(
                    Status.Sent,
                    storage.getNewestMessages(chatId = "chat", limit = 1).single()
                        .recipients.single { it.userId == "peer" }.status,
                )
            }
        }

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

    private fun outgoing(status: Status): MessageSM.Text =
        incoming(authorId = "me").copy(
            recipients = listOf(
                MessageRecipientSM("me", Status.Sent),
                MessageRecipientSM("peer", status),
                MessageRecipientSM("other", Status.Sent),
            ),
        )

    private suspend fun ChatStorage.read(
        chatId: String = "chat",
        messageId: String = "message",
        authorId: String = "me",
        readByUserId: String = "peer",
    ): Boolean =
        markMessageRead(
            chatId = chatId,
            messageId = messageId,
            authorId = authorId,
            readByUserId = readByUserId,
        )
}
