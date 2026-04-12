package com.security.chat.multiplatform.features.chats.ui.screens.chatlist

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.domain.asLceState
import com.security.chat.multiplatform.common.core.domain.startOnSubscribe
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.features.chats.domain.ChatsModel
import com.security.chat.multiplatform.features.chats.ui.screens.chatlist.entity.Chats
import com.security.chat.multiplatform.features.chats.ui.screens.chatlist.mapper.toUi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class ChatListViewModel(
    private val chatsModel: ChatsModel,
) : BaseViewModel<ChatListState, ChatListEvent>() {

    override fun onPostStart() {
        super.onPostStart()

        chatsModel.fetchChatsList.jobFlow.asLceState()
            .onEach { state ->
                updateState { it.copy(chatListIsLoading = state.isLoading) }
            }
            .launchIn(viewModelScope)

        chatsModel.getChatListFlow()
            .onEach { chats ->
                val newChatItems = chats
                    .map { chat ->
                        chat.toUi()
                    }

                val oldChats = currentViewState.chats
                val newChats = oldChats.copy(items = newChatItems)

                updateState { it.copy(chats = newChats) }
            }
            .launchIn(viewModelScope)

        chatsModel.fetchChatsList.startOnSubscribe()
    }

    override fun createInitialState(): ChatListState {
        return ChatListState(
            chatListIsLoading = false,
            chats = Chats.EMPTY,
        )
    }

    fun onRefreshChatsTriggered() {
        chatsModel.fetchChatsList.startOnSubscribe()
    }

}