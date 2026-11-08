package com.security.chat.multiplatform.features.chat.ui.screens.groupchat

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chat.domain.GroupChatModel
import com.security.chat.multiplatform.features.chat.domain.entity.ChatInfo
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.entity.MessageQuote
import com.security.chat.multiplatform.features.chat.domain.entity.PickedImage
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageStatusUM
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageUM
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Test
import ru.kode.remo.Task0
import ru.kode.remo.Task1
import java.lang.reflect.Proxy
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupChatViewModelTest {
    @Test
    fun `deletion requires confirmation cancellation is harmless and duplicate confirmation is ignored`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val model = FakeModel(dispatcher)
            model.start(parentScope = backgroundScope)
            val vm = GroupChatViewModel(
                groupChatModel = model,
                params = stub { name ->
                    check(name == "getChatId")
                    "chat"
                },
                pushModel = stub { error("Unexpected push call: $it") },
                analytics = stub { error("Unexpected analytics call: $it") },
                dispatcherProvider = object : DispatcherProviderInterface {
                    override val Default = dispatcher
                    override val IO = dispatcher
                    override val Unconfined = dispatcher
                    override val Main = Dispatchers.Main
                },
            )
            try {
                var state = vm.viewState.value
                vm.viewState.onEach { state = it }.launchIn(backgroundScope)
                runCurrent()
                val message = MessageUM.Outgoing.Text(
                    id = "message",
                    text = "text",
                    datetimeText = "12:10",
                    status = MessageStatusUM.Sent,
                )
                vm.onMessageClicked(message)
                vm.onDeleteMessageClicked()
                runCurrent()
                assertTrue(model.deletedIds.isEmpty())
                assertNull(state.messageMenuId)
                assertNotNull(state.alertDialogDescriptor?.negativeAction).invoke()
                runCurrent()
                assertNull(state.alertDialogDescriptor)
                assertTrue(model.deletedIds.isEmpty())

                vm.onMessageClicked(message)
                vm.onDeleteMessageClicked()
                runCurrent()
                val confirm = assertNotNull(state.alertDialogDescriptor?.positiveAction)
                confirm()
                confirm()
                runCurrent()
                assertEquals(listOf("message"), model.deletedIds)
                assertEquals("message", state.deletingMessageId)
                assertNull(state.alertDialogDescriptor)
                model.deletionCompleted.complete(Unit)
                runCurrent()
                assertNull(state.deletingMessageId)

                model.failure = IllegalStateException("Offline")
                vm.onMessageClicked(message)
                vm.onDeleteMessageClicked()
                runCurrent()
                assertNotNull(state.alertDialogDescriptor?.positiveAction).invoke()
                runCurrent()
                assertNull(state.deletingMessageId)
                assertNotNull(state.alertDialogDescriptor)
                assertNull(state.alertDialogDescriptor?.negativeAction)
            } finally {
                vm.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    private class FakeModel(dispatcher: CoroutineDispatcher) : BaseModel(dispatcher),
        GroupChatModel {
        val deletedIds = mutableListOf<String>()
        val deletionCompleted = CompletableDeferred<Unit>()
        var failure: Throwable? = null

        override val deleteMessage: Task1<String, Unit> =
            task { id ->
                failure?.let { throw it }
                deletedIds += id
                deletionCompleted.await()
            }
        override val sendImage: Task1<PickedImage, Unit> = task { _ -> }
        override val sendMessage: Task0<Unit> = task { -> }
        override val syncMessages: Task0<Unit> = task { -> }
        override val markMessagesReadUpTo: Task1<String, Unit> = task { _ -> }

        override fun setChatId(id: String) = Unit
        override fun setCurrentMessageText(text: String) = Unit
        override fun getCurrentMessageFlow(): Flow<String> = emptyFlow()
        override fun getMessagesPager(initialMessageId: String?): Flow<PagingData<Message>> {
            return emptyFlow()
        }

        override fun setReplyQuote(quote: MessageQuote?) = Unit

        override fun getReplyQuoteFlow(): Flow<MessageQuote?> = emptyFlow()
        override fun onViewActive() = Unit
        override fun onViewInactive() = Unit
        override fun getChatInfoFlow(): Flow<ChatInfo?> = emptyFlow()
    }
}

private inline fun <reified T> stub(noinline invoke: (String) -> Any?): T =
    T::class.java.cast(
        Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, method, _ -> invoke(method.name) },
    )
