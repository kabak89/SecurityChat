package com.security.chat.multiplatform.features.chat.data.common

import com.security.chat.multiplatform.common.core.error.NetworkError
import com.security.chat.multiplatform.features.chat.data.network.entity.network.BaseMessageRequest
import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageRecipientSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import com.security.chat.multiplatform.features.chat.data.storage.entity.Status
import com.security.chat.multiplatform.features.users.data.storage.entity.UserSM
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SendOutgoingMessagesWorkerTest {

    @Test
    fun `a failed send stays queued until another run and does not block other chats`() = runTest {
        val pending = listOf(
            message(id = "first", chatId = "chat-a"),
            message(id = "next", chatId = "chat-b"),
        )
            .associateByTo(linkedMapOf()) { it.id }
        val attempts = mutableListOf<String>()
        val storage = stub<ChatStorage> { name, args ->
            when (name) {
                "migrateMessageStatuses" -> Unit
                "getUnsentMessages" -> pending.values.toList()
                "getMessage" -> pending[args[1]]
                "isMessageDeletionPending" -> false
                "markMessageSent" -> {
                    pending.remove(args[0])
                    Unit
                }

                else -> error("Unexpected storage call: $name")
            }
        }
        val worker = worker(storage) { request ->
            attempts += request.id
            if (attempts.size == 1) throw NetworkError(statusCode = 503)
        }

        worker.run(mapOf(OUTGOING_MESSAGES_USER_ID to "different-user"))
        assertEquals(emptyList(), attempts)

        worker.run(mapOf(OUTGOING_MESSAGES_USER_ID to "me"))
        assertEquals(listOf("first", "next"), attempts)
        assertEquals(listOf("first"), pending.keys.toList())

        worker.run(mapOf(OUTGOING_MESSAGES_USER_ID to "me"))
        assertEquals(listOf("first", "next", "first"), attempts)
        assertEquals(emptyList(), pending.keys.toList())
    }

    @Test
    fun `deletion queued after reading the batch prevents that send`() = runTest {
        val toDelete = message(id = "to-delete", chatId = "chat-a")
        val other = message(id = "next", chatId = "chat-b")
        val attempts = mutableListOf<String>()
        val markedSent = mutableListOf<String>()
        val sendDeleteMutex = Mutex(locked = true)
        var batchRead = false
        var deletionPending = false
        val storage = stub<ChatStorage> { name, args ->
            when (name) {
                "migrateMessageStatuses" -> Unit
                "getUnsentMessages" -> {
                    batchRead = true
                    listOf(toDelete, other)
                }

                "isMessageDeletionPending" -> {
                    assertEquals("me", args[2])
                    deletionPending && args[0] == toDelete.chatId && args[1] == toDelete.id
                }

                "getMessage" -> listOf(toDelete, other).single { it.id == args[1] }
                "markMessageSent" -> {
                    markedSent += args[0] as String
                    Unit
                }

                else -> error("Unexpected storage call: $name")
            }
        }
        val worker = worker(storage = storage, sendDeleteMutex = sendDeleteMutex) {
            attempts += it.id
        }

        val send = launch { worker.run(mapOf(OUTGOING_MESSAGES_USER_ID to "me")) }
        runCurrent()
        assertTrue(batchRead)
        deletionPending = true
        sendDeleteMutex.unlock()
        send.join()

        assertEquals(listOf(other.id), attempts)
        assertEquals(listOf(other.id), markedSent)
    }

    private fun worker(
        storage: ChatStorage,
        sendDeleteMutex: Mutex = Mutex(),
        sendMessage: (BaseMessageRequest) -> Unit,
    ): SendOutgoingMessagesWorker {
        return SendOutgoingMessagesWorker(
            chatStorage = storage,
            userStorage = stub { name, _ ->
                check(name == "getUserId")
                "me"
            },
            usersStorage = stub { name, _ ->
                check(name == "getUser")
                UserSM(id = "peer", publicKey = "public", name = "Peer")
            },
            usersNetworkManager = stub { name, _ -> error("Unexpected users network call: $name") },
            chatNetworkManager = stub { name, args ->
                check(name == "sendMessage")
                sendMessage(args[0] as BaseMessageRequest)
                null
            },
            chatDataHelper = stub { name, _ ->
                when (name) {
                    "getOneTimeEncryptionKey", "encryptKey", "encryptText" -> "encrypted"
                    else -> error("Unexpected encryption call: $name")
                }
            },
            fileManager = stub { name, _ -> error("Unexpected file call: $name") },
            scheduler = stub { name, _ ->
                check(name == "getSendDeleteMutex")
                sendDeleteMutex
            },
        )
    }

    private fun message(id: String, chatId: String): MessageSM.Text {
        return MessageSM.Text(
            id = id,
            chatId = chatId,
            recipients = listOf(MessageRecipientSM(userId = "peer", status = Status.Created)),
            authorId = "me",
            timestamp = 1,
            text = "text",
        )
    }
}

private inline fun <reified T> stub(noinline invoke: (String, Array<out Any?>) -> Any?): T {
    return T::class.java.cast(
        Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, method, args ->
            invoke(method.name, args.orEmpty())
        },
    )
}
