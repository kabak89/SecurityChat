package com.security.chat.multiplatform.features.chat.domain

import androidx.paging.PagingData
import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.domain.startOnSubscribe
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chat.domain.entity.ChatInfo
import com.security.chat.multiplatform.features.chat.domain.entity.Message
import com.security.chat.multiplatform.features.chat.domain.entity.MessageQuote
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
    public val deleteMessage: Task1<String, Unit>
    public val markMessagesReadUpTo: Task1<String, Unit>

    public fun setChatId(id: String)
    public fun setCurrentMessageText(text: String)
    public fun getCurrentMessageFlow(): Flow<String>
    public fun getMessagesPager(initialMessageId: String? = null): Flow<PagingData<Message>>
    public fun onViewActive()
    public fun onViewInactive()
    public fun getChatInfoFlow(): Flow<ChatInfo?>
    public fun setReplyQuote(quote: MessageQuote?)
    public fun getReplyQuoteFlow(): Flow<MessageQuote?>
}

internal class GroupChatModelImpl(
    private val chatRepo: ChatRepo,
    dispatcherProvider: DispatcherProviderInterface,
) : GroupChatModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
    ) {

    private val stateFlow: MutableStateFlow<State> = MutableStateFlow(State())

    override val sendImage: Task1<PickedImage, Unit> =
        task { photo ->
            val draft = stateFlow.value
            val chatId = checkNotNull(draft.chatId)
            val cachedPhoto = chatRepo.copyImageToCache(photo)
            val messageDescriptor = chatRepo.createEncryptedFile(
                file = cachedPhoto,
                chatId = chatId,
            )
            chatRepo.saveImageMessage(
                chatId = chatId,
                message = messageDescriptor,
                replyToMessageId = draft.replyQuote?.messageId,
            )
            stateFlow.update { current ->
                if (current.replyQuote === draft.replyQuote) {
                    current.copy(replyQuote = null)
                } else {
                    current
                }
            }
            chatRepo.uploadMessages(chatId = chatId)
        }

    override val sendMessage: Task0<Unit> =
        task { ->
            val draft = stateFlow.value
            if (draft.currentMessage.isBlank()) return@task

            val chatId = checkNotNull(draft.chatId)
            chatRepo.saveTextMessage(
                message = draft.currentMessage,
                chatId = chatId,
                replyToMessageId = draft.replyQuote?.messageId,
            )

            stateFlow.update { current ->
                val text = if (current.currentMessage == draft.currentMessage) {
                    ""
                } else {
                    current.currentMessage
                }
                current.copy(
                    currentMessage = text,
                    replyQuote = current.replyQuote.takeUnless { it === draft.replyQuote },
                )
            }

            chatRepo.uploadMessages(chatId = chatId)
        }

    override val syncMessages: Task0<Unit> =
        task { ->
            val chatId = checkNotNull(stateFlow.value.chatId)
            chatRepo.fetchMessages(chatId = chatId)
        }

    override val deleteMessage: Task1<String, Unit> =
        task { messageId ->
            chatRepo.deleteMessage(
                chatId = checkNotNull(stateFlow.value.chatId),
                messageId = messageId,
            )
        }

    override val markMessagesReadUpTo: Task1<String, Unit> =
        task { messageId ->
            val state = stateFlow.value
            val chatId = state.chatId ?: return@task
            if (!state.isActive) return@task
            chatRepo.markMessagesReadUpTo(chatId = chatId, messageId = messageId)
        }

    override fun onPostStart() {
        super.onPostStart()

        stateFlow
            .map { it.chatId to it.isActive }
            .distinctUntilChanged()
            .flatMapLatest { (chatId, isActive) ->
                if ((chatId != null) && isActive) {
                    channelFlow<Unit> {
                        launch {
                            syncMessages.startOnSubscribe().join()
                            launch { chatRepo.subscribeToNewMessages(chatId) }
                            launch { chatRepo.subscribeToEvents(chatId) }
                        }
                        launch { chatRepo.setUserOnline() }
                    }
                } else {
                    emptyFlow()
                }
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

    override fun getMessagesPager(initialMessageId: String?): Flow<PagingData<Message>> {
        return stateFlow
            .map { it.chatId }
            .distinctUntilChanged()
            .filterNotNull()
            .flatMapLatest { chatId ->
                chatRepo.getMessagesPager(chatId = chatId, initialMessageId = initialMessageId)
            }
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

    override fun setReplyQuote(quote: MessageQuote?) {
        stateFlow.update { it.copy(replyQuote = quote) }
    }

    override fun getReplyQuoteFlow(): Flow<MessageQuote?> {
        return stateFlow.map { it.replyQuote }.distinctUntilChanged()
    }

    private data class State(
        val currentMessage: String = "",
        val replyQuote: MessageQuote? = null,
        val chatId: String? = null,
        val isActive: Boolean = false,
    )
}
