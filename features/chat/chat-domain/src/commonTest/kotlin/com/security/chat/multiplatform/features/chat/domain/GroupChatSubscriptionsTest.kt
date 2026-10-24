package com.security.chat.multiplatform.features.chat.domain

import androidx.paging.PagingData
import com.security.chat.multiplatform.common.core.domain.asLceState
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chat.domain.entity.ChatInfo
import com.security.chat.multiplatform.features.chat.domain.entity.FileDescriptor
import com.security.chat.multiplatform.features.chat.domain.entity.ImageMessageDescriptor
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.entity.PickedImage
import com.security.chat.multiplatform.features.chat.domain.repo.ChatRepo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupChatSubscriptionsTest {
    @Test
    fun `both subscriptions start after HTTP sync and stop when inactive`() =
        runTest {
            val repo = FakeChatRepo()
            val model = startModel(repo)
            model.onViewActive()
            runCurrent()
            assertEquals(1, repo.syncCount)
            assertFalse(repo.messagesActive)
            assertFalse(repo.eventsActive)

            repo.syncCompleted.complete(Unit)
            runCurrent()
            assertTrue(repo.messagesActive)
            assertTrue(repo.eventsActive)

            model.onViewInactive()
            runCurrent()
            assertFalse(repo.messagesActive)
            assertFalse(repo.eventsActive)
        }

    @Test
    fun `leaving during HTTP sync prevents both subscriptions from starting afterward`() =
        runTest {
            val repo = FakeChatRepo()
            val model = startModel(repo)
            model.onViewActive()
            runCurrent()
            model.onViewInactive()
            runCurrent()
            repo.syncCompleted.complete(Unit)
            runCurrent()
            assertFalse(repo.messagesActive)
            assertFalse(repo.eventsActive)
            assertEquals(0, repo.subscriptionCount)
        }

    private fun TestScope.startModel(repo: FakeChatRepo): GroupChatModel {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val model = GroupChatModelImpl(
            chatRepo = repo,
            dispatcherProvider = object : DispatcherProviderInterface {
                override val Default = dispatcher
                override val IO = dispatcher
                override val Unconfined = dispatcher
                override val Main = Dispatchers.Main
            },
        )
        model.start(parentScope = backgroundScope)
        model.syncMessages.jobFlow.asLceState().launchIn(backgroundScope)
        model.setChatId(id = "chat")
        return model
    }

    private class FakeChatRepo : ChatRepo {
        val syncCompleted = CompletableDeferred<Unit>()
        var syncCount = 0
        var subscriptionCount = 0
        var messagesActive = false
        var eventsActive = false

        override suspend fun fetchMessages(chatId: String) {
            syncCount++
            syncCompleted.await()
        }

        override suspend fun subscribeToNewMessages(chatId: String) {
            subscriptionCount++
            messagesActive = true
            try {
                awaitCancellation()
            } finally {
                messagesActive = false
            }
        }

        override suspend fun subscribeToEvents(chatId: String) {
            subscriptionCount++
            eventsActive = true
            try {
                awaitCancellation()
            } finally {
                eventsActive = false
            }
        }

        override suspend fun setUserOnline(): Unit = awaitCancellation()
        override suspend fun saveTextMessage(message: String, chatId: String) = Unit
        override suspend fun saveImageMessage(
            chatId: String,
            message: ImageMessageDescriptor,
        ) =
            Unit

        override suspend fun uploadMessages(chatId: String) = Unit
        override suspend fun markMessagesReadUpTo(chatId: String, messageId: String) = Unit
        override fun getMessagesPager(chatId: String): Flow<PagingData<Message>> = emptyFlow()
        override fun getChatInfoFlow(chatId: String): Flow<ChatInfo?> = emptyFlow()
        override suspend fun copyImageToCache(image: PickedImage): FileDescriptor = error("Unused")
        override suspend fun createEncryptedFile(
            file: FileDescriptor,
            chatId: String,
        ): ImageMessageDescriptor =
            error("Unused")
    }
}
