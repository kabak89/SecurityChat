package com.security.chat.multiplatform.features.chat.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.kode.remo.Task0
import kotlin.time.Duration.Companion.seconds

public interface ChatModel : ScopedModel {
    public val sendMessage: Task0<Unit>

    public fun setCurrentMessageText(text: String)
    public fun setCurrentMessageFlow(): Flow<String>
}

internal class ChatModelImpl(
//    private val chatsRepo: ChatsRepo,
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

            delay(2.seconds)

            stateFlow.update { it.copy(currentMessage = "") }
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
    )

}