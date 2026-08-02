package com.security.chat.multiplatform.features.chat.ui.screens.groupchat

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import com.security.chat.multiplatform.common.core.domain.asLceState
import com.security.chat.multiplatform.common.core.domain.startOnSubscribe
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import com.security.chat.multiplatform.common.core.ui.mappers.toUiLceState
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogContent
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor
import com.security.chat.multiplatform.features.chat.component.api.GroupChatComponent
import com.security.chat.multiplatform.features.chat.domain.GroupChatModel
import com.security.chat.multiplatform.features.chat.domain.entity.PickedImage
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageUM
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.mapper.groupChatErrorMapper
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.mapper.isNotImageError
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.mapper.toUi
import com.security.chat.multiplatform.features.push.domain.PushModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import securitychat.common.localization.generated.resources.common_close

internal class GroupChatViewModel(
    private val groupChatModel: GroupChatModel,
    private val params: GroupChatComponent,
    private val pushModel: PushModel,
) : BaseViewModel<GroupChatState, GroupChatEvent>() {

    internal val messages: Flow<PagingData<MessageUM>> =
        groupChatModel.getMessagesPager()
            .map { pagingData ->
                pagingData
                    .map { it.toUi() }
                    .filter { it !is MessageUM.Nothing }
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
                    pushModel.clearNotificationsForChat(chatId = params.chatId)
                    groupChatModel.syncMessages.startOnSubscribe()
                    groupChatModel.onViewActive()
                    pushModel.setShowNotificationsForChat(chatId = params.chatId, show = false)
                } else {
                    groupChatModel.onViewInactive()
                    pushModel.setShowNotificationsForChat(chatId = params.chatId, show = true)
                }
            }
            .launchIn(viewModelScope)

        groupChatModel.sendImage.jobFlow.asLceState()
            .map { it.toUiLceState(::groupChatErrorMapper) }
            .filterIsInstance<UiLceState.Error>()
            .onEach { error ->
                val alertDialogDescriptor = if (error.error.cause.isNotImageError()) {
                    val content = AlertDialogContent(
                        title = error.error.title,
                        message = error.error.description,
                        positiveButtonText = resPrintableText(StringRes.common_close),
                    )
                    AlertDialogDescriptor(
                        content = content,
                        dismissAction = { updateState { it.copy(alertDialogDescriptor = null) } },
                        positiveAction = {
                            updateState { it.copy(alertDialogDescriptor = null) }
                        },
                    )
                } else {
                    val alertDialogContent = AlertDialogContent(
                        title = error.error.title,
                        message = error.error.description,
                        positiveButtonText = resPrintableText(StringRes.common_close),
                    )

                    AlertDialogDescriptor(
                        content = alertDialogContent,
                        positiveAction = { updateState { it.copy(alertDialogDescriptor = null) } },
                        dismissAction = { updateState { it.copy(alertDialogDescriptor = null) } },
                    )
                }
                updateState { it.copy(alertDialogDescriptor = alertDialogDescriptor) }
            }
            .launchIn(viewModelScope)
    }

    override fun createInitialState(): GroupChatState {
        return GroupChatState(
            message = "",
            syncState = UiLceState.NotStarted,
            alertDialogDescriptor = null,
        )
    }

    fun onMessageEdited(newText: String) {
        groupChatModel.setCurrentMessageText(text = newText)
    }

    fun onImagePicked(photo: PickedImage) {
        groupChatModel.sendImage.startOnSubscribe(photo)
    }

    fun onSendMessageClicked() {
        //TODO startOnSubscribe()
        groupChatModel.sendMessage.start()
    }

    fun onSyncClicked() {
        groupChatModel.syncMessages.startOnSubscribe()
    }
}
