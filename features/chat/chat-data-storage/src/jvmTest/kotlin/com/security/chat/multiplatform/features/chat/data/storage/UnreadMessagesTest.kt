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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UnreadMessagesTest {
    @Test
    fun `unread query filters by user and chat and orders incoming messages chronologically`() =
        runTest {
            withStorage { storage ->
                storage.saveMessages(
                    listOf(
                        message(id = "newer", timestamp = 3),
                        message(id = "same-time-b", timestamp = 2),
                        message(id = "read", timestamp = 1, status = Status.Read),
                        message(id = "other-chat", timestamp = 1, chatId = "other-chat"),
                        message(id = "outgoing", timestamp = 1, authorId = "me"),
                        message(id = "other-user", timestamp = 1, recipientId = "other-user"),
                        message(id = "sent", timestamp = 1, status = Status.Sent),
                        message(id = "same-time-a", timestamp = 2),
                        message(id = "oldest", timestamp = 1),
                    ),
                )

                assertEquals(
                    listOf("oldest", "same-time-a", "same-time-b", "newer"),
                    storage.getUnreadMessages(chatId = "chat", userId = "me").map { it.id },
                )
                assertTrue(storage.getUnreadMessages(chatId = "missing", userId = "me").isEmpty())
            }
        }

    @Test
    fun `deliveries retain unread messages without duplicates and exclude read messages`() =
        runTest {
            withStorage { storage ->
                val first = message(id = "first", timestamp = 1)
                val second = message(id = "second", timestamp = 2)
                storage.saveReceivedMessages(messages = listOf(first), receivedByUserId = "me")
                storage.saveReceivedMessages(
                    messages = listOf(second, first),
                    receivedByUserId = "me",
                )
                assertEquals(
                    listOf("first", "second"),
                    storage.getUnreadMessages(chatId = "chat", userId = "me").map { it.id },
                )

                storage.markMessagesReadUpTo(chatId = "chat", messageId = "first", userId = "me")
                storage.saveReceivedMessages(messages = listOf(first), receivedByUserId = "me")
                assertEquals(
                    listOf("second"),
                    storage.getUnreadMessages(chatId = "chat", userId = "me").map { it.id },
                )

                storage.markMessagesReadUpTo(chatId = "chat", messageId = "second", userId = "me")
                assertTrue(storage.getUnreadMessages(chatId = "chat", userId = "me").isEmpty())
            }
        }

    @Test
    fun `unread messages include images and all messages beyond a page`() =
        runTest {
            withStorage { storage ->
                val messages = (1..100).map { message(id = "message-$it", timestamp = it.toLong()) }
                val image = MessageSM.Image(
                    id = "image",
                    chatId = "chat",
                    recipients = listOf(
                        MessageRecipientSM(userId = "me", status = Status.ReceivedByReceiver),
                    ),
                    authorId = "peer",
                    timestamp = 101,
                    fileId = "file",
                    key = "key",
                    isDownloaded = true,
                )
                storage.saveMessages(messages + image)

                val unread = storage.getUnreadMessages(chatId = "chat", userId = "me")
                assertEquals(messages + image, unread)
                assertIs<MessageSM.Image>(unread.last())
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

    private fun message(
        id: String,
        timestamp: Long,
        chatId: String = "chat",
        authorId: String = "peer",
        recipientId: String = "me",
        status: Status = Status.ReceivedByReceiver,
    ): MessageSM.Text =
        MessageSM.Text(
            id = id,
            chatId = chatId,
            recipients = listOf(MessageRecipientSM(userId = recipientId, status = status)),
            authorId = authorId,
            timestamp = timestamp,
            text = id,
        )
}
