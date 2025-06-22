package com.security.chat.multiplatform.features.chats.ui.screens.addchat

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.domain.asLceState
import com.security.chat.multiplatform.common.core.domain.startOnSubscribe
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.features.chats.domain.ChatsModel
import com.security.chat.multiplatform.features.chats.domain.entity.CreateChatResult
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.kode.remo.successResults

internal class AddChatViewModel(
    private val chatsModel: ChatsModel,
) : BaseViewModel<AddChatState, AddChatEvent>() {

    override fun onPostStart() {
        super.onPostStart()

        chatsModel.getAddChatStateFlow()
            .onEach { state ->
                updateState { it.copy(username = state.username) }
            }
            .launchIn(viewModelScope)

        chatsModel.createChat.jobFlow.asLceState()
            .onEach { state ->
                updateState { it.copy(isLoading = state.isLoading) }
            }
            .launchIn(viewModelScope)

        chatsModel.createChat.jobFlow.successResults()
            .onEach { result ->
                when (result) {
                    is CreateChatResult.ChatCreated -> {
                        sendEvent(AddChatEvent.ChatCreated(id = result.id))
                    }

                    is CreateChatResult.UserNotFound -> {
                        //TODO
                        println("UserNotFound")
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    override fun createInitialState(): AddChatState {
        return AddChatState(
            username = "",
            isLoading = false,
        )
    }

    fun onUsernameTextChanged(newUsernameText: String) {
        chatsModel.setUsername(newUsernameText)
    }

    fun onFindClicked() {
        chatsModel.createChat.startOnSubscribe()
    }

}