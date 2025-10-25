package com.security.chat.multiplatform.features.chat.ui.screens.personalchat

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.domain.asLceState
import com.security.chat.multiplatform.common.core.domain.startOnSubscribe
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.features.chat.component.PersonalChatComponent
import com.security.chat.multiplatform.features.chat.domain.ChatModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class PersonalChatViewModel(
    private val chatModel: ChatModel,
    private val params: PersonalChatComponent,
) : BaseViewModel<PersonalChatState, PersonalChatEvent>() {

    override fun onPostStart() {
        super.onPostStart()

        chatModel.setChatId(id = params.chatId)

        chatModel.setCurrentMessageFlow()
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
    }

    override fun createInitialState(): PersonalChatState {
        return PersonalChatState(
            message = "",
            sendingMessageInProgress = false,
        )
    }

    fun onMessageEdited(message: String) {
        chatModel.setCurrentMessageText(text = message)
    }

    fun onSendMessageClicked() {
        chatModel.sendMessage.startOnSubscribe()
    }

}