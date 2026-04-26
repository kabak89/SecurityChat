package com.security.chat.multiplatform.features.chat.domain

import androidx.paging.PagingData
import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chat.domain.entity.Interlocutor
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.repo.ChatRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kode.remo.Task0

public interface ChatModel : ScopedModel {
    public val sendMessage: Task0<Unit>
    public val syncMessages: Task0<Unit>
    public val fetchCompanionInfo: Task0<Unit>

    public fun setChatId(id: String)
    public fun setCurrentMessageText(text: String)
    public fun getCurrentMessageFlow(): Flow<String>
    public fun getMessagesPager(): Flow<PagingData<Message>>
    public fun onViewActive()
    public fun onViewInactive()
    public fun getInterlocutorInfoFlow(): Flow<Interlocutor?>
}

internal class ChatModelImpl(
    private val chatRepo: ChatRepo,
    dispatcherProvider: DispatcherProviderInterface,
) : ChatModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
    ) {

    private val stateFlow: MutableStateFlow<State> = MutableStateFlow(State())

    private var newMessagesJob: Job? = null
    private var publishOnlineStatusJob: Job? = null

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

    override val syncMessages: Task0<Unit> =
        task { ->
            val chatId = checkNotNull(stateFlow.value.chatId)
            chatRepo.fetchMessages(chatId = chatId)
        }

    override val fetchCompanionInfo: Task0<Unit> =
        task { ->
            val chatId = checkNotNull(stateFlow.value.chatId)
            chatRepo.fetchCompanionInfo(chatId = chatId)
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

    override fun getMessagesPager(): Flow<PagingData<Message>> {
        return stateFlow
            .map { it.chatId }
            .distinctUntilChanged()
            .filterNotNull()
            .flatMapLatest { chatId -> chatRepo.getMessagesPager(chatId) }
    }

    override fun onViewActive() {
        newMessagesJob = scope.launch {
            val chatId = stateFlow.map { it.chatId }.filterNotNull().first()
            chatRepo.subscribeToNewMessages(chatId)
        }

        publishOnlineStatusJob = scope.launch {
            chatRepo.setUserOnline()
        }
    }

    override fun onViewInactive() {
        newMessagesJob?.cancel()
        publishOnlineStatusJob?.cancel()
    }

    override fun getInterlocutorInfoFlow(): Flow<Interlocutor?> {
        val chatId = checkNotNull(stateFlow.value.chatId)
        return chatRepo.getInterlocutorInfoFlow(chatId)
    }

    private data class State(
        val currentMessage: String = "",
        val chatId: String? = null,
    )
}