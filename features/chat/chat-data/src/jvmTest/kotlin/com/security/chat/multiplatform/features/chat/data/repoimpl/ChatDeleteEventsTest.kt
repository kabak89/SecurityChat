package com.security.chat.multiplatform.features.chat.data.repoimpl

import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chat.data.network.ChatNetworkManager
import com.security.chat.multiplatform.features.chat.data.network.entity.ChatEventNM
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.domain.entity.error.MessageDeletionError
import com.security.chat.multiplatform.features.chat.domain.repo.ChatRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChatDeleteEventsTest {
    @Test
    fun `deletions and duplicates are acknowledged after local persistence`() =
        runTest {
            val actions = mutableListOf<String>()
            val event = deletedEvent()
            val repo = repo(
                events = listOf(event, event),
                deleteLocally = { chatId, messageId, authorId ->
                    assertEquals(
                        listOf("chat", "message", "peer"),
                        listOf(chatId, messageId, authorId),
                    )
                    actions += "deleted"
                },
                acknowledge = { actions += "ack" },
            )
            repo.subscribeToEvents("chat")
            assertEquals(listOf("deleted", "ack", "deleted", "ack"), actions)
        }

    @Test
    fun `failed persistence is not acknowledged and other chat events are ignored`() =
        runTest {
            val actions = mutableListOf<String>()
            val repo = repo(
                events = listOf(
                    deletedEvent(),
                    deletedEvent().copy(chatId = "other"),
                    deletedEvent().copy(id = "next", messageId = "next"),
                ),
                deleteLocally = { _, messageId, _ ->
                    if (messageId == "message") error("Database unavailable")
                    actions += messageId
                },
                acknowledge = { actions += "ack" },
            )
            repo.subscribeToEvents("chat")
            assertEquals(listOf("next", "ack"), actions)
        }

    @Test
    fun `deletion is queued before contacting the server and applied after acceptance`() =
        runTest {
            val actions = mutableListOf<String>()
            val repo = repo(
                message = message("me"),
                enqueueDeletion = {
                    actions += "queued"
                    true
                },
                requestDeletion = { actions += "request" },
                deleteLocally = { _, _, authorId ->
                    assertEquals("me", authorId)
                    actions += "deleted"
                },
            )
            repo.deleteMessage(chatId = "chat", messageId = "message")
            assertEquals(listOf("queued", "request", "deleted"), actions)
        }

    @Test
    fun `another author is rejected and network failures leave the queued deletion pending`() =
        runTest {
            var deletions = 0
            var queued = false
            val wrongAuthor = repo(
                message = message("peer"),
                enqueueDeletion = { error("Must not queue deletion") },
                requestDeletion = { error("Must not request deletion") },
                deleteLocally = { _, _, _ -> deletions++ },
            )
            assertFailsWith<MessageDeletionError> {
                wrongAuthor.deleteMessage(chatId = "chat", messageId = "message")
            }
            val failed = repo(
                message = message("me"),
                enqueueDeletion = {
                    queued = true
                    true
                },
                requestDeletion = {
                    assertEquals(true, queued)
                    error("Offline")
                },
                deleteLocally = { _, _, _ -> deletions++ },
            )
            assertFailsWith<IllegalStateException> {
                failed.deleteMessage(chatId = "chat", messageId = "message")
            }
            assertEquals(0, deletions)
        }

    @Test
    fun `deletion never contacts the server if the database write fails`() =
        runTest {
            val repo = repo(
                message = message("me"),
                enqueueDeletion = { error("Database unavailable") },
                requestDeletion = { throw AssertionError("Must not request deletion") },
            )
            val error = assertFailsWith<IllegalStateException> {
                repo.deleteMessage(chatId = "chat", messageId = "message")
            }
            assertEquals("Database unavailable", error.message)
        }

    private fun TestScope.repo(
        events: List<ChatEventNM> = emptyList(),
        message: MessageSM? = null,
        enqueueDeletion: () -> Boolean = { true },
        requestDeletion: () -> Unit = {},
        deleteLocally: (String, String, String) -> Unit = { _, _, _ -> },
        acknowledge: (ChatEventNM) -> Unit = {},
    ): ChatRepo {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return ChatRepoImpl(
            usersNetworkManager = unused(),
            userStorage = stub { name, _ ->
                assertEquals("getUserId", name)
                "me"
            },
            usersStorage = unused(),
            chatsStorage = unused(),
            chatStorage = stub { name, args ->
                when (name) {
                    "getMessage" -> message
                    "enqueueMessageDeletion" -> {
                        assertEquals(listOf("chat", "message", "me"), args.take(3))
                        enqueueDeletion()
                    }

                    else -> error("Unexpected storage call: $name")
                }
            },
            timeProvider = unused(),
            dispatcherProvider = object : DispatcherProviderInterface {
                override val Default = dispatcher
                override val IO = dispatcher
                override val Unconfined = dispatcher
                override val Main = Dispatchers.Main
            },
            chatNetworkManager = stub<ChatNetworkManager> { name, args ->
                when (name) {
                    "getEventsFlow" -> flowOf(*events.toTypedArray())
                    "acknowledgeEvent" -> acknowledge(args[0] as ChatEventNM)
                    "deleteMessage" -> requestDeletion()
                    else -> error("Unexpected network call: $name")
                }
            },
            chatDataHelper = stub { name, args ->
                when (name) {
                    "deleteMessage" -> deleteLocally(
                        args[0] as String,
                        args[1] as String,
                        args[2] as String,
                    )

                    "migrateMessageStatuses", "restoreOutgoingMessageRecipients" -> Unit
                    else -> error("Unexpected helper call: $name")
                }
            },
            fileManager = unused(),
            chatsDataHelper = unused(),
            chatPresenceHelper = unused(),
            outgoingMessageScheduler = stub { name, _ ->
                assertEquals("getSendDeleteMutex", name)
                Mutex()
            },
            chatEventsPublisher = stub { _, _ -> Unit },
        )
    }

    private fun message(authorId: String): MessageSM.Text =
        MessageSM.Text(
            id = "message",
            chatId = "chat",
            recipients = emptyList(),
            authorId = authorId,
            timestamp = 1,
            text = "text",
        )

    private fun deletedEvent(): ChatEventNM.MessageDeleted =
        ChatEventNM.MessageDeleted(
            id = "event",
            connectionId = 1,
            messageId = "message",
            chatId = "chat",
            deletedByUserId = "peer",
        )
}

private inline fun <reified T> unused(): T = stub { name, _ -> error("Unexpected call: $name") }

private inline fun <reified T> stub(noinline invoke: (String, Array<out Any?>) -> Any?): T =
    T::class.java.cast(
        Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, method, args -> invoke(method.name, args.orEmpty()) },
    )
