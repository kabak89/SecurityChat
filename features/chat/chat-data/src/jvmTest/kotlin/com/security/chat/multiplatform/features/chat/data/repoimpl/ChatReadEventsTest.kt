package com.security.chat.multiplatform.features.chat.data.repoimpl

import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chat.data.network.ChatNetworkManager
import com.security.chat.multiplatform.features.chat.data.network.entity.ChatEventNM
import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.chat.domain.repo.ChatRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatReadEventsTest {
    @Test
    fun `read receipts and their duplicates are acknowledged after storing the recipient status`() =
        runTest {
            val actions = mutableListOf<String>()
            val event = readEvent()
            val repo = repo(
                events = listOf(event, event),
                storage = stub { name, args ->
                    assertEquals("markMessageRead", name)
                    assertEquals(listOf("chat", "message", "me", "peer"), args.take(4))
                    actions += "stored"
                    true
                },
                acknowledge = { actions += "ack" },
            )
            repo.subscribeToEvents("chat")
            assertEquals(listOf("stored", "ack", "stored", "ack"), actions)
        }

    @Test
    fun `read receipts from another chat and unmatched recipients are not acknowledged`() =
        runTest {
            var writes = 0
            val repo = repo(
                events = listOf(readEvent().copy(chatId = "other"), readEvent()),
                storage = stub { name, _ ->
                    assertEquals("markMessageRead", name)
                    writes++
                    false
                },
                acknowledge = { error("Invalid receipt must not be acknowledged") },
            )
            repo.subscribeToEvents("chat")
            assertEquals(1, writes)
        }

    @Test
    fun `a failed read status write remains unacknowledged and does not block the next receipt`() =
        runTest {
            val stored = mutableListOf<String>()
            val acknowledged = mutableListOf<String>()
            val repo = repo(
                events = listOf(readEvent(), readEvent().copy(id = "next", messageId = "next")),
                storage = stub { _, args ->
                    if (args[1] == "message") error("Database unavailable")
                    stored += args[1] as String
                    true
                },
                acknowledge = { acknowledged += it.id },
            )
            repo.subscribeToEvents("chat")
            assertEquals(listOf("next"), stored)
            assertEquals(listOf("next"), acknowledged)
        }

    @Test
    fun `local reading saves the read queue before starting the publisher`() =
        runTest {
            val actions = mutableListOf<String>()
            val repo = repo(
                storage = stub { name, args ->
                    assertEquals("markMessagesReadUpTo", name)
                    assertEquals(listOf("chat", "message", "me"), args.take(3))
                    actions += "stored"
                    Unit
                },
                startPublisher = { actions += "publisher" },
            )
            repo.markMessagesReadUpTo(chatId = "chat", messageId = "message")
            assertEquals(listOf("stored", "publisher"), actions)
        }

    private fun TestScope.repo(
        storage: ChatStorage,
        events: List<ChatEventNM> = emptyList(),
        acknowledge: (ChatEventNM) -> Unit = {},
        startPublisher: () -> Unit = {},
    ): ChatRepo {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return ChatRepoImpl(
            networkManagerFactory = unused(),
            userStorage = stub { name, _ ->
                assertEquals("getUserId", name)
                "me"
            },
            usersStorage = unused(),
            chatsStorage = unused(),
            chatStorage = storage,
            timeProvider = unused(),
            networkConfig = NetworkConfig(host = "localhost", port = 0),
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
                    else -> error("Unexpected network call: $name")
                }
            },
            chatDataHelper = stub { name, _ ->
                assertTrue(
                    name in listOf("migrateMessageStatuses", "restoreOutgoingMessageRecipients"),
                )
                Unit
            },
            fileManager = unused(),
            json = Json,
            chatsDataHelper = unused(),
            chatPresenceHelper = unused(),
            chatEventsPublisher = stub { name, args ->
                assertEquals("start", name)
                assertEquals("chat", args[0])
                startPublisher()
            },
        )
    }

    private fun readEvent(): ChatEventNM.MessageRead =
        ChatEventNM.MessageRead(
            id = "event",
            connectionId = 1,
            messageId = "message",
            chatId = "chat",
            readByUserId = "peer",
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
