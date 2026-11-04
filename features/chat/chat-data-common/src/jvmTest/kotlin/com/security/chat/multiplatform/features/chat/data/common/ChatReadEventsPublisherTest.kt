package com.security.chat.multiplatform.features.chat.data.common

import com.security.chat.multiplatform.features.chat.data.network.ChatNetworkManager
import com.security.chat.multiplatform.features.chat.data.network.entity.ChatEventNM
import com.security.chat.multiplatform.features.chat.data.network.entity.ChatMessageNM
import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageReadEventSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageReceivedEventSM
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatReadEventsPublisherTest {
    @Test
    fun `stopping cancels an in flight receipt and clears restoration state`() =
        runTest {
            val fixture = Fixture()
            fixture.read.value = listOf(fixture.receipt())
            val acknowledgement = CompletableDeferred<Unit>()
            fixture.readAcknowledgement = acknowledgement
            val publisher = fixture.publisher(backgroundScope)
            publisher.start("chat")
            runCurrent()

            publisher.stop()
            fixture.read.value = emptyList()
            acknowledgement.complete(Unit)
            advanceTimeBy(60_000)
            runCurrent()
            assertTrue(fixture.publishedReadIds.isEmpty())
            assertEquals(listOf("read"), fixture.publications)

            fixture.readAcknowledgement = null
            fixture.read.value = listOf(fixture.receipt())
            publisher.start("chat")
            runCurrent()
            assertEquals(listOf("read", "read"), fixture.publications)
            assertEquals(listOf("chat" to "me", "chat" to "me"), fixture.restoredReadChats)
            assertEquals(listOf("read"), fixture.publishedReadIds)
        }

    @Test
    fun `publisher observes new read receipts alongside delivery and restores each chat once`() =
        runTest {
            val fixture = Fixture()
            val publisher = fixture.publisher(backgroundScope)
            publisher.start("chat")
            publisher.start("chat")
            runCurrent()

            fixture.received.value = listOf(fixture.delivery())
            fixture.read.value = listOf(fixture.receipt())
            runCurrent()

            assertEquals(listOf("delivery", "read"), fixture.publications)
            assertEquals(listOf("chat" to "me"), fixture.restoredReadChats)
            assertTrue(fixture.received.value.isEmpty())
            assertTrue(fixture.read.value.isEmpty())
        }

    @Test
    fun `read receipt remains pending until the server acknowledges publication`() =
        runTest {
            val fixture = Fixture()
            fixture.read.value = listOf(fixture.receipt())
            val acknowledgement = CompletableDeferred<Unit>()
            fixture.readAcknowledgement = acknowledgement
            fixture.publisher(backgroundScope).start("chat")
            runCurrent()
            assertEquals(1, fixture.read.value.size)
            assertTrue(fixture.publishedReadIds.isEmpty())

            acknowledgement.complete(Unit)
            runCurrent()
            assertTrue(fixture.read.value.isEmpty())
            assertEquals(listOf("read"), fixture.publishedReadIds)
        }

    @Test
    fun `failed read receipts retry without another message or opening another chat`() =
        runTest {
            val fixture = Fixture()
            fixture.read.value = listOf(fixture.receipt())
            fixture.readFailures = 1
            fixture.publisher(backgroundScope).start("chat")
            runCurrent()
            assertEquals(listOf("read"), fixture.publications)
            assertEquals(1, fixture.read.value.size)

            advanceTimeBy(5_000)
            runCurrent()
            assertEquals(listOf("read", "read"), fixture.publications)
            assertTrue(fixture.read.value.isEmpty())
        }

    @Test
    fun `failed delivery does not prevent successful read receipts from publishing`() =
        runTest {
            val fixture = Fixture()
            fixture.received.value = listOf(fixture.delivery())
            fixture.read.value = listOf(fixture.receipt())
            fixture.deliveryFails = true
            fixture.publisher(backgroundScope).start("chat")
            runCurrent()

            assertEquals(1, fixture.received.value.size)
            assertTrue(fixture.read.value.isEmpty())
            assertEquals(listOf("read"), fixture.publishedReadIds)
        }

    @Test
    fun `restarting a publisher resumes an unacknowledged read receipt with the same id`() =
        runTest {
            val fixture = Fixture()
            fixture.read.value = listOf(fixture.receipt())
            fixture.readAcknowledgement = CompletableDeferred()
            val firstScope = CoroutineScope(
                backgroundScope.coroutineContext +
                        SupervisorJob(backgroundScope.coroutineContext[Job]),
            )
            fixture.publisher(firstScope).start("chat")
            runCurrent()
            firstScope.cancel()
            runCurrent()
            assertEquals(1, fixture.read.value.size)

            fixture.readAcknowledgement = null
            fixture.publisher(backgroundScope).start("chat")
            runCurrent()
            assertEquals(listOf("read", "read"), fixture.publications)
            assertEquals(listOf("read"), fixture.publishedReadIds)
            assertTrue(fixture.read.value.isEmpty())
        }

    @Test
    fun `logout stops queued read receipts and switching accounts publishes only their receipts`() =
        runTest {
            val fixture = Fixture()
            fixture.read.value = listOf(fixture.receipt(), fixture.receipt(userId = "other"))
            fixture.readFailures = 1
            val publisher = fixture.publisher(backgroundScope)
            publisher.start("chat")
            runCurrent()
            fixture.userId = null
            advanceTimeBy(5_000)
            runCurrent()
            assertEquals(listOf("read"), fixture.publications)
            assertEquals(2, fixture.read.value.size)

            fixture.userId = "other"
            publisher.start("chat")
            runCurrent()
            assertEquals(listOf("read", "read-other"), fixture.publications)
            assertEquals(listOf("read"), fixture.read.value.map { it.id })
        }

    private class Fixture {
        val received = MutableStateFlow<List<MessageReceivedEventSM>>(emptyList())
        val read = MutableStateFlow<List<MessageReadEventSM>>(emptyList())
        val restoredReadChats = mutableListOf<Pair<String, String>>()
        val publications = mutableListOf<String>()
        val publishedReadIds = mutableListOf<String>()
        var userId: String? = "me"
        var readFailures = 0
        var deliveryFails = false
        var readAcknowledgement: CompletableDeferred<Unit>? = null

        val storage = stub<ChatStorage> { name, args ->
            when (name) {
                "enqueueStoredReceivedEvents" -> Unit
                "enqueueStoredReadEvents" -> {
                    restoredReadChats += (args[0] as String) to (args[1] as String)
                    Unit
                }

                "observePendingReceivedEvents" -> received.map { events ->
                    events.filter { it.receivedByUserId == args[0] }
                }

                "observePendingReadEvents" -> read.map { events ->
                    events.filter { it.readByUserId == args[0] }
                }

                "markReceivedEventPublished" -> {
                    received.value = received.value.filterNot { it.id == args[0] }
                    Unit
                }

                "markReadEventPublished" -> {
                    publishedReadIds += args[0] as String
                    read.value = read.value.filterNot { it.id == args[0] }
                    Unit
                }

                else -> error("Unexpected storage call: $name")
            }
        }

        val userStorage = stub<UserStorage> { name, _ ->
            check(name == "getUserId") { "Unexpected user storage call: $name" }
            userId
        }

        val network = object : ChatNetworkManager {
            override suspend fun publishMessageReceivedEvent(
                eventId: String,
                messageId: String,
                chatId: String,
                recipientUserId: String,
                receivedByUserId: String,
            ) {
                publications += eventId
                check(!deliveryFails) { "Offline" }
            }

            override suspend fun publishMessageReadEvent(
                eventId: String,
                messageId: String,
                chatId: String,
                recipientUserId: String,
                readByUserId: String,
            ) {
                assertEquals("message", messageId)
                assertEquals("chat", chatId)
                assertEquals("author", recipientUserId)
                assertEquals(userId, readByUserId)
                publications += eventId
                if (readFailures > 0) {
                    readFailures--
                    error("Offline")
                }
                readAcknowledgement?.await()
            }

            override suspend fun getMessages(chatId: String): List<ChatMessageNM> = error("Unused")
            override suspend fun getNewMessagesFlow(
                chatId: String,
                authorId: String,
                deviceId: String,
            ): Flow<ChatMessageNM> =
                error("Unused")

            override suspend fun processNewMessages(
                serializedMessages: String,
            ): List<ChatMessageNM> =
                error("Unused")

            override fun getEventsFlow(chatId: String): Flow<ChatEventNM> = error("Unused")
            override suspend fun acknowledgeEvent(event: ChatEventNM): Unit = error("Unused")
            override suspend fun confirmReceivingMessages(
                chatId: String,
                deviceId: String,
                messageIds: List<String>,
            ): Unit =
                error("Unused")

            override suspend fun downloadFile(fileId: String, destinationPath: String): Unit =
                error("Unused")

            override suspend fun deleteMessage(chatId: String, messageId: String) = Unit
        }

        fun publisher(scope: CoroutineScope): ChatEventsPublisher =
            ChatEventsPublisherImpl(
                chatStorage = storage,
                chatNetworkManager = network,
                userStorage = userStorage,
                coroutineScope = scope,
            )

        fun receipt(userId: String = "me"): MessageReadEventSM =
            MessageReadEventSM(
                id = if (userId == "me") "read" else "read-$userId",
                messageId = "message",
                chatId = "chat",
                recipientUserId = "author",
                readByUserId = userId,
            )

        fun delivery(): MessageReceivedEventSM =
            MessageReceivedEventSM(
                id = "delivery",
                messageId = "message",
                chatId = "chat",
                recipientUserId = "author",
                receivedByUserId = "me",
            )
    }
}

private inline fun <reified T> stub(noinline invoke: (String, Array<out Any?>) -> Any?): T =
    T::class.java.cast(
        Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, method, args -> invoke(method.name, args.orEmpty()) },
    )
