package com.security.chat.multiplatform.features.chat.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chat.domain.repo.ChatRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.kode.remo.Task0

public interface ChatModel : ScopedModel {
    public val sendMessage: Task0<Unit>

    public fun setChatId(id: String)
    public fun setCurrentMessageText(text: String)
    public fun setCurrentMessageFlow(): Flow<String>
}

internal class ChatModelImpl(
    private val chatRepo: ChatRepo,
    coroutineScope: CoroutineScope,
    dispatcherProvider: DispatcherProviderInterface,
) : ChatModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
        coroutineScope = coroutineScope,
    ) {

    private val stateFlow: MutableStateFlow<State> = MutableStateFlow(State())

    override val sendMessage: Task0<Unit> =
        task { ->
            val currentMessage = stateFlow.value.currentMessage
            if (currentMessage.isBlank()) return@task

            val chatId = checkNotNull(stateFlow.value.chatId)

            chatRepo.sendMessage(
                message = currentMessage,
                chatId = chatId,
            )

            stateFlow.update { it.copy(currentMessage = "") }
        }

    override fun setChatId(id: String) {
        stateFlow.update { it.copy(chatId = id) }
    }

    override fun setCurrentMessageText(text: String) {
        stateFlow.update { it.copy(currentMessage = text) }
    }

    override fun setCurrentMessageFlow(): Flow<String> {
        return stateFlow
            .map { it.currentMessage }
            .distinctUntilChanged()
    }

    private data class State(
        val currentMessage: String = "",
        val chatId: String? = null,
    )

}