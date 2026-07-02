package com.security.chat.multiplatform.features.chat.ui.screens.personalchat

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.security.chat.multiplatform.common.core.domain.asLceState
import com.security.chat.multiplatform.common.core.domain.startOnSubscribe
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState
import com.security.chat.multiplatform.common.core.ui.mappers.toUiLceState
import com.security.chat.multiplatform.features.chat.component.api.PersonalChatComponent
import com.security.chat.multiplatform.features.chat.domain.PersonalChatModel
import com.security.chat.multiplatform.features.chat.ui.screens.personalchat.entity.MessageUM
import com.security.chat.multiplatform.features.chat.ui.screens.personalchat.mapper.toUi
import com.security.chat.multiplatform.features.push.domain.PushModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

internal class PersonalChatViewModel(
    private val personalChatModel: PersonalChatModel,
    private val params: PersonalChatComponent,
    private val pushModel: PushModel,
) : BaseViewModel<PersonalChatState, PersonalChatEvent>() {

    internal val messages: Flow<PagingData<MessageUM>> =
        personalChatModel.getMessagesPager()
            .map { pagingData -> pagingData.map { it.toUi() } }
            .cachedIn(viewModelScope)

    override fun onPostStart() {
        super.onPostStart()

        personalChatModel.setChatId(id = params.chatId)

        personalChatModel.getCurrentMessageFlow()
            .onEach { currentMessage ->
                updateState { it.copy(message = currentMessage) }
            }
            .launchIn(viewModelScope)

        personalChatModel.fetchCompanionInfo.start()

        personalChatModel.sendMessage.jobFlow
            .asLceState()
            .onEach { state ->
                updateState { it.copy(sendingMessageInProgress = state.isLoading) }
            }
            .launchIn(viewModelScope)

        personalChatModel.syncMessages.jobFlow.asLceState().map { it.toUiLceState() }
            .onEach { syncState ->
                updateState { it.copy(syncState = syncState) }
            }
            .launchIn(viewModelScope)

        viewActivable.activeFlow
            .onEach { active ->
                if (active) {
                    pushModel.clearNotificationsForChat(chatId = params.chatId)
                    personalChatModel.syncMessages.startOnSubscribe()
                    personalChatModel.onViewActive()
                    pushModel.setShowNotificationsForChat(chatId = params.chatId, show = false)
                } else {
                    personalChatModel.onViewInactive()
                    pushModel.setShowNotificationsForChat(chatId = params.chatId, show = true)
                }
            }
            .launchIn(viewModelScope)

        personalChatModel.getInterlocutorInfoFlow()
            .filterNotNull()
            .collectWhenViewActive()
            .onEach { interlocutor ->
                updateState { it.copy(interlocutor = interlocutor.toUi()) }
            }
            .launchIn(viewModelScope)
    }

    override fun createInitialState(): PersonalChatState {
        return PersonalChatState(
            message = "",
            sendingMessageInProgress = false,
            syncState = UiLceState.NotStarted,
            interlocutor = null,
        )
    }

    override fun onCleared() {
        super.onCleared()
        pushModel.setShowNotificationsForChat(chatId = params.chatId, show = true)
    }

    fun onMessageEdited(message: String) {
        personalChatModel.setCurrentMessageText(text = message)
    }

    fun onSendMessageClicked() {
        personalChatModel.sendMessage.startOnSubscribe()
    }

    fun onSyncClicked() {
        personalChatModel.syncMessages.startOnSubscribe()
    }
}
