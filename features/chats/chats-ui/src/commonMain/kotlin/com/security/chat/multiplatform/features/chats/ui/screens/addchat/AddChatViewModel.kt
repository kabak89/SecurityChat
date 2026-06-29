package com.security.chat.multiplatform.features.chats.ui.screens.addchat

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.domain.asLceState
import com.security.chat.multiplatform.common.core.domain.startOnSubscribe
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState
import com.security.chat.multiplatform.common.core.ui.entity.isLoading
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import com.security.chat.multiplatform.common.core.ui.mappers.toUiLceState
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogContent
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor
import com.security.chat.multiplatform.features.chats.domain.CreateChatModel
import com.security.chat.multiplatform.features.chats.domain.entity.CreateChatResult
import com.security.chat.multiplatform.features.chats.ui.screens.addchat.entity.ChatDescriptor
import com.security.chat.multiplatform.features.chats.ui.screens.addchat.entity.ChatType
import com.security.chat.multiplatform.features.chats.ui.screens.addchat.mapper.createChatErrorMapper
import com.security.chat.multiplatform.features.chats.ui.screens.addchat.mapper.isNotFoundError
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ru.kode.remo.successResults
import securitychat.common.localization.generated.resources.common_close
import securitychat.common.localization.generated.resources.common_retry

internal class AddChatViewModel(
    private val createChatModel: CreateChatModel,
) : BaseViewModel<AddChatState, AddChatEvent>() {

    override fun onPostStart() {
        super.onPostStart()

        createChatModel.getAddChatStateFlow()
            .onEach { state ->
                updateState { oldState ->
                    oldState.copy(
                        personalChat = oldState.personalChat.copy(
                            username = state.personalChatUsername,
                        ),
                    )
                }
            }
            .launchIn(viewModelScope)

        createChatModel.createPersonalChat.jobFlow.asLceState()
            .map { it.toUiLceState(::createChatErrorMapper) }
            .onEach { state ->
                updateState { oldState ->
                    oldState.copy(
                        personalChat = oldState.personalChat.copy(
                            isLoading = state.isLoading,
                        ),
                    )
                }

                val alertDialogDescriptor = when (state) {
                    is UiLceState.Loading,
                    is UiLceState.NotStarted,
                    is UiLceState.Ready,
                        -> null

                    is UiLceState.Error -> {
                        if (state.error.cause.isNotFoundError()) {
                            val alertDialogContent = AlertDialogContent(
                                title = state.error.title,
                                message = state.error.description,
                                positiveButtonText = resPrintableText(StringRes.common_close),
                            )

                            AlertDialogDescriptor(
                                content = alertDialogContent,
                                positiveAction = { updateState { it.copy(dialogDescriptor = null) } },
                                dismissAction = { updateState { it.copy(dialogDescriptor = null) } },
                            )
                        } else {
                            val alertDialogContent = AlertDialogContent(
                                title = state.error.title,
                                message = state.error.description,
                                positiveButtonText = resPrintableText(StringRes.common_retry),
                                negativeButtonText = resPrintableText(StringRes.common_close),
                            )

                            AlertDialogDescriptor(
                                content = alertDialogContent,
                                positiveAction = { createChatModel.createPersonalChat.startOnSubscribe() },
                                negativeAction = { updateState { it.copy(dialogDescriptor = null) } },
                                dismissAction = { updateState { it.copy(dialogDescriptor = null) } },
                            )
                        }
                    }
                }

                updateState { it.copy(dialogDescriptor = alertDialogDescriptor) }
            }
            .launchIn(viewModelScope)

        createChatModel.createPersonalChat.jobFlow.successResults()
            .onEach { result ->
                when (result) {
                    is CreateChatResult.ChatCreated -> {
                        sendEvent(AddChatEvent.ChatCreated(id = result.id))
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    override fun createInitialState(): AddChatState {
        return AddChatState(
            personalChat = ChatDescriptor.Personal(
                username = "",
                isLoading = false,
            ),
            groupChat = ChatDescriptor.Group(
                username = "",
                isLoading = false,
                addedUsers = emptyList(),
            ),
            activeType = ChatType.Personal,
            dialogDescriptor = null,
        )
    }

    fun onPersonalChatUsernameChanged(newUsernameText: String) {
        createChatModel.setPersonalChatUsername(newUsernameText)
    }

    fun onPersonalChatFindClicked() {
        createChatModel.createPersonalChat.startOnSubscribe()
    }

    fun onTypeSelected(chatType: ChatType) {
        updateState { it.copy(activeType = chatType) }
    }
}