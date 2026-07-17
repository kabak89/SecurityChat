package com.security.chat.multiplatform.features.chat.ui.screens.groupchat

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.security.chat.multiplatform.common.core.domain.asLceState
import com.security.chat.multiplatform.common.core.domain.startOnSubscribe
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState
import com.security.chat.multiplatform.common.core.ui.mappers.toUiLceState
import com.security.chat.multiplatform.features.chat.component.api.GroupChatComponent
import com.security.chat.multiplatform.features.chat.domain.GroupChatModel
import com.security.chat.multiplatform.features.chat.domain.entity.PickedPhoto
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageUM
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.mapper.toUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

internal class GroupChatViewModel(
    private val groupChatModel: GroupChatModel,
    private val params: GroupChatComponent,
) : BaseViewModel<GroupChatState, GroupChatEvent>() {

    internal val messages: Flow<PagingData<MessageUM>> =
        groupChatModel.getMessagesPager()
            .map { pagingData ->
                pagingData.map { it.toUi() }
            }
            .cachedIn(viewModelScope)

    override fun onPostStart() {
        super.onPostStart()

        groupChatModel.setChatId(id = params.chatId)

        groupChatModel.getCurrentMessageFlow()
            .onEach { currentMessage ->
                updateState { it.copy(message = currentMessage) }
            }
            .launchIn(viewModelScope)

        groupChatModel.syncMessages.jobFlow.asLceState().map { it.toUiLceState() }
            .onEach { syncState ->
                updateState { it.copy(syncState = syncState) }
            }
            .launchIn(viewModelScope)

        viewActivable.activeFlow
            .onEach { active ->
                if (active) {
                    groupChatModel.syncMessages.startOnSubscribe()
                    groupChatModel.onViewActive()
                } else {
                    groupChatModel.onViewInactive()
                }
            }
            .launchIn(viewModelScope)
    }

    override fun createInitialState(): GroupChatState {
        return GroupChatState(
            message = "",
            syncState = UiLceState.NotStarted,
        )
    }

    fun onMessageEdited(newText: String) {
        groupChatModel.setCurrentMessageText(text = newText)
    }

    fun onPhotoPicked(photo: PickedPhoto) {
        groupChatModel.cachePhoto.start(photo)
    }

    fun onSendMessageClicked() {
        //TODO startOnSubscribe()
        groupChatModel.sendMessage.start()
    }

    fun onSyncClicked() {
        groupChatModel.syncMessages.startOnSubscribe()
    }
}
