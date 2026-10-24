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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MessageDeliveryStatusTest {
    @Test
    fun `delivery updates only the matching recipient and notifies message observers`() =
        runTest {
            withStorage { storage ->
                storage.saveMessage(message())
                val changed = backgroundScope.async {
                    storage.observeMessagesChanges("chat").drop(1).first()
                }
                runCurrent()

                assertTrue(storage.receive())
                changed.await()
                val recipients = storage.getNewestMessages(chatId = "chat", limit = 1)
                    .single().recipients.associate { it.userId to it.status }
                assertEquals(Status.ReceivedByReceiver, recipients["peer"])
                assertEquals(Status.Sent, recipients["other"])
                assertEquals(Status.Sent, recipients["me"])
            }
        }

    @Test
    fun `duplicate delivery and sent updates preserve received and read statuses`() =
        runTest {
            withStorage { storage ->
                val statuses = listOf(
                    Status.Created,
                    Status.Sent,
                    Status.ReceivedByReceiver,
                    Status.Read,
                )
                for (initial in statuses) {
                    storage.clearAll()
                    storage.saveMessage(message(status = initial))
                    assertTrue(storage.receive())
                    assertTrue(storage.receive())
                    storage.markMessageSent(id = "message")
                    storage.saveMessage(message(status = Status.Sent))

                    val status = storage.getNewestMessages(chatId = "chat", limit = 1)
                        .single().recipients.single { it.userId == "peer" }.status
                    assertEquals(
                        if (initial == Status.Read) Status.Read else Status.ReceivedByReceiver,
                        status,
                    )
                }
            }
        }

    @Test
    fun `delivery ignores another author chat recipient and missing messages`() =
        runTest {
            withStorage { storage ->
                storage.saveMessage(message())
                assertFalse(storage.receive(authorId = "other"))
                assertFalse(storage.receive(chatId = "another-chat"))
                assertFalse(storage.receive(receivedByUserId = "unknown"))
                assertFalse(storage.receive(receivedByUserId = "me"))
                assertFalse(storage.receive(messageId = "missing"))
                val status = storage.getNewestMessages(chatId = "chat", limit = 1)
                    .single().recipients.single { it.userId == "peer" }.status
                assertEquals(Status.Sent, status)
            }
        }

    @Test
    fun `delivery also updates image message recipients`() =
        runTest {
            withStorage { storage ->
                storage.saveMessage(
                    MessageSM.Image(
                        id = "message",
                        chatId = "chat",
                        recipients = listOf(
                            MessageRecipientSM(
                                userId = "peer",
                                status = Status.Sent,
                            ),
                        ),
                        authorId = "me",
                        timestamp = 1,
                        fileId = "file",
                        key = "key",
                        isDownloaded = true,
                    ),
                )
                assertTrue(storage.receive())
                assertEquals(
                    Status.ReceivedByReceiver,
                    storage.getNewestMessages(chatId = "chat", limit = 1).single()
                        .recipients.single().status,
                )
            }
        }

    private suspend fun TestScope.withStorage(block: suspend (ChatStorage) -> Unit) {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            ChatDb.Schema.create(driver).await()
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
            block(storage)
        } finally {
            driver.close()
        }
    }

    private fun message(status: Status = Status.Sent): MessageSM =
        MessageSM.Text(
            id = "message",
            chatId = "chat",
            recipients = listOf(
                MessageRecipientSM(userId = "me", status = Status.Sent),
                MessageRecipientSM(userId = "peer", status = status),
                MessageRecipientSM(userId = "other", status = Status.Sent),
            ),
            authorId = "me",
            timestamp = 1,
            text = "text",
        )

    private suspend fun ChatStorage.receive(
        chatId: String = "chat",
        messageId: String = "message",
        authorId: String = "me",
        receivedByUserId: String = "peer",
    ): Boolean =
        markMessageReceived(
            chatId = chatId,
            messageId = messageId,
            authorId = authorId,
            receivedByUserId = receivedByUserId,
        )
}
