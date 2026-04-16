package com.security.chat.multiplatform.features.chats.ui.screens.chatlist

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.domain.asLceState
import com.security.chat.multiplatform.common.core.domain.startOnSubscribe
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import com.security.chat.multiplatform.common.core.ui.mappers.toUiLceState
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogContent
import com.security.chat.multiplatform.features.chats.domain.ChatsModel
import com.security.chat.multiplatform.features.chats.ui.screens.chatlist.entity.Chats
import com.security.chat.multiplatform.features.chats.ui.screens.chatlist.mapper.toUi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import securitychat.common.localization.generated.resources.common_close
import securitychat.common.localization.generated.resources.common_retry

internal class ChatListViewModel(
    private val chatsModel: ChatsModel,
) : BaseViewModel<ChatListState, ChatListEvent>() {

    override fun onPostStart() {
        super.onPostStart()

        chatsModel.fetchChatsList.jobFlow
            .asLceState()
            .map { it.toUiLceState() }
            .onEach { state ->
                val errorDialogContent = when (state) {
                    is UiLceState.Loading,
                    is UiLceState.NotStarted,
                    is UiLceState.Ready,
                        -> null

                    is UiLceState.Error -> AlertDialogContent(
                        title = state.error.title,
                        message = state.error.description,
                        positiveButtonText = resPrintableText(StringRes.common_retry),
                        negativeButtonText = resPrintableText(StringRes.common_close),
                    )
                }
                updateState {
                    it.copy(
                        loadingState = state,
                        errorDialogContent = errorDialogContent,
                    )
                }
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
            loadingState = UiLceState.NotStarted,
            chats = Chats.EMPTY,
            errorDialogContent = null,
        )
    }

    fun onRefreshChatsTriggered() {
        chatsModel.fetchChatsList.startOnSubscribe()
    }

    fun onCloseErrorDialogClicked() {
        updateState { it.copy(errorDialogContent = null) }
    }

    fun onReloadChatsClicked() {
        chatsModel.fetchChatsList.startOnSubscribe()
    }
}