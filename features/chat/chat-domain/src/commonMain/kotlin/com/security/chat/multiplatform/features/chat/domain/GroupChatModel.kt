package com.security.chat.multiplatform.features.chat.domain

import androidx.paging.PagingData
import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chat.domain.entity.ChatInfo
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.entity.PickedImage
import com.security.chat.multiplatform.features.chat.domain.repo.ChatRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.kode.remo.Task0
import ru.kode.remo.Task1

public interface GroupChatModel : ScopedModel {
    public val sendImage: Task1<PickedImage, Unit>
    public val sendMessage: Task0<Unit>
    public val syncMessages: Task0<Unit>

    public fun setChatId(id: String)
    public fun setCurrentMessageText(text: String)
    public fun getCurrentMessageFlow(): Flow<String>
    public fun getMessagesPager(): Flow<PagingData<Message>>
    public fun onViewActive()
    public fun onViewInactive()
    public fun getChatInfoFlow(): Flow<ChatInfo?>
}

internal class GroupChatModelImpl(
    private val chatRepo: ChatRepo,
    dispatcherProvider: DispatcherProviderInterface,
) : GroupChatModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
    ) {

    private val stateFlow: MutableStateFlow<State> = MutableStateFlow(State())

    override fun onPostStart() {
        super.onPostStart()

        stateFlow
            .map { it.chatId to it.isActive }
            .distinctUntilChanged()
            .flatMapLatest { (chatId, isActive) ->
                if ((chatId != null) && isActive) {
                    channelFlow<Unit> {
                        launch { chatRepo.subscribeToNewMessages(chatId) }
                        launch { chatRepo.setUserOnline() }
                    }
                } else {
                    emptyFlow()
                }
            }
            .launchIn(scope)
    }

    override val sendImage: Task1<PickedImage, Unit> =
        task { photo ->
            val cachedPhoto = chatRepo.copyImageToCache(photo)
            val chatId = checkNotNull(stateFlow.value.chatId)
            val messageDescriptor = chatRepo.createEncryptedFile(
                file = cachedPhoto,
                chatId = chatId,
            )
            chatRepo.saveImageMessage(chatId = chatId, message = messageDescriptor)
            chatRepo.uploadMessages(chatId = chatId)
        }

    override val sendMessage: Task0<Unit> =
        task { ->
            val currentMessage = stateFlow.value.currentMessage
            if (currentMessage.isBlank()) return@task

            val chatId = checkNotNull(stateFlow.value.chatId)

            chatRepo.saveTextMessage(
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
        stateFlow.update { it.copy(isActive = true) }
    }

    override fun onViewInactive() {
        stateFlow.update { it.copy(isActive = false) }
    }

    override fun getChatInfoFlow(): Flow<ChatInfo?> {
        return stateFlow
            .map { it.chatId to it.isActive }
            .distinctUntilChanged()
            .flatMapLatest { (chatId, isActive) ->
                if ((chatId != null) && isActive) {
                    chatRepo.getChatInfoFlow(chatId)
                } else {
                    flowOf(null)
                }
            }
    }

    private data class State(
        val currentMessage: String = "",
        val chatId: String? = null,
        val isActive: Boolean = false,
    )
}