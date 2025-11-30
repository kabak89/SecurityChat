package com.security.chat.multiplatform.features.chat.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.repo.ChatRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import ru.kode.remo.Task0

public interface ChatModel : ScopedModel {
    public val sendMessage: Task0<Unit>

    public fun setChatId(id: String)
    public fun setCurrentMessageText(text: String)
    public fun getCurrentMessageFlow(): Flow<String>
    public fun getMessagesFlow(): Flow<List<Message>>
}

internal class ChatModelImpl(
    private val chatRepo: ChatRepo,
    dispatcherProvider: DispatcherProviderInterface,
) : ChatModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
    ) {

    private val stateFlow: MutableStateFlow<State> = MutableStateFlow(State())

    override val sendMessage: Task0<Unit> =
        task { ->
            val currentMessage = stateFlow.value.currentMessage
            if (currentMessage.isBlank()) return@task

            val chatId = checkNotNull(stateFlow.value.chatId)

            chatRepo.saveMessage(
                message = currentMessage,
                chatId = chatId,
            )

            stateFlow.update { it.copy(currentMessage = "") }

            chatRepo.uploadMessages(chatId = chatId)
        }

    override fun onPostStart() {
        super.onPostStart()

        stateFlow
            .map { it.chatId }
            .filterNotNull()
            .take(1)
            .onEach { chatId ->
                chatRepo.fetchMessages(chatId = chatId)
            }
            .launchIn(scope)
    }

    override fun setChatId(id: String) {
        if (stateFlow.value.chatId != null) {
            error("Do not change chat id")
        }

        stateFlow.update { it.copy(chatId = id) }
    }

    override fun setCurrentMessageText(text: String) {
        stateFlow.update { it.copy(currentMessage = text) }
    }

    override fun getCurrentMessageFlow(): Flow<String> {
        return stateFlow
            .map { it.currentMessage }
            .distinctUntilChanged()
    }

    override fun getMessagesFlow(): Flow<List<Message>> {
        val chatId = checkNotNull(stateFlow.value.chatId)

        return chatRepo.getMessagesFlow(
            chatId = chatId,
        )
    }

    private data class State(
        val currentMessage: String = "",
        val chatId: String? = null,
    )

}