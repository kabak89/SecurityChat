package com.security.chat.multiplatform.features.chat.ui.screens.personalchat

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.domain.asLceState
import com.security.chat.multiplatform.common.core.domain.startOnSubscribe
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.features.chat.component.PersonalChatComponent
import com.security.chat.multiplatform.features.chat.domain.ChatModel
import com.security.chat.multiplatform.features.chat.ui.screens.personalchat.mapper.toUi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class PersonalChatViewModel(
    private val chatModel: ChatModel,
    private val params: PersonalChatComponent,
) : BaseViewModel<PersonalChatState, PersonalChatEvent>() {

    override fun onPostStart() {
        super.onPostStart()

        chatModel.setChatId(id = params.chatId)

        chatModel.getCurrentMessageFlow()
            .onEach { currentMessage ->
                updateState { it.copy(message = currentMessage) }
            }
            .launchIn(viewModelScope)

        chatModel.sendMessage.jobFlow
            .asLceState()
            .onEach { state ->
                updateState { it.copy(sendingMessageInProgress = state.isLoading) }
            }
            .launchIn(viewModelScope)

        chatModel.getMessagesFlow()
            .onEach { messages ->
                val newMessages = messages.map { it.toUi() }
                updateState { it.copy(messages = newMessages) }
            }
            .launchIn(viewModelScope)
    }

    override fun createInitialState(): PersonalChatState {
        return PersonalChatState(
            message = "",
            sendingMessageInProgress = false,
            messages = emptyList(),
        )
    }

    fun onMessageEdited(message: String) {
        chatModel.setCurrentMessageText(text = message)
    }

    fun onSendMessageClicked() {
        chatModel.sendMessage.startOnSubscribe()
    }

}