package com.security.chat.multiplatform.features.add_chat.ui

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
import com.security.chat.multiplatform.features.add_chat.domain.CreateChatModel
import com.security.chat.multiplatform.features.add_chat.ui.entity.AddedUser
import com.security.chat.multiplatform.features.add_chat.ui.entity.ChatDescriptor
import com.security.chat.multiplatform.features.add_chat.ui.entity.ChatType
import com.security.chat.multiplatform.features.add_chat.ui.mapper.createChatErrorMapper
import com.security.chat.multiplatform.features.add_chat.ui.mapper.isNotFoundError
import com.security.chat.multiplatform.features.add_chat.ui.mapper.isSameUserError
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
                val addedUsers = state.chatMembers.map {
                    AddedUser(id = it.id, username = it.username)
                }

                updateState { oldState ->
                    oldState.copy(
                        personalChat = oldState.personalChat.copy(
                            username = state.personalChatUsername,
                        ),
                        groupChat = oldState.groupChat.copy(
                            username = state.groupChatUsername,
                            addedUsers = addedUsers,
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
                sendEvent(AddChatEvent.PersonalChatCreated(id = result.id))
            }
            .launchIn(viewModelScope)

        createChatModel.findUserForGroupChat.jobFlow.asLceState()
            .map { it.toUiLceState(::createChatErrorMapper) }
            .onEach { state ->
                val alertDialogDescriptor = when (state) {
                    is UiLceState.Loading,
                    is UiLceState.NotStarted,
                    is UiLceState.Ready,
                        -> null

                    is UiLceState.Error -> {
                        when {
                            state.error.cause.isNotFoundError() -> {
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
                            }

                            state.error.cause.isSameUserError() -> {
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
                            }

                            else -> {
                                val alertDialogContent = AlertDialogContent(
                                    title = state.error.title,
                                    message = state.error.description,
                                    positiveButtonText = resPrintableText(StringRes.common_retry),
                                    negativeButtonText = resPrintableText(StringRes.common_close),
                                )

                                AlertDialogDescriptor(
                                    content = alertDialogContent,
                                    positiveAction = { createChatModel.findUserForGroupChat.startOnSubscribe() },
                                    negativeAction = { updateState { it.copy(dialogDescriptor = null) } },
                                    dismissAction = { updateState { it.copy(dialogDescriptor = null) } },
                                )
                            }
                        }
                    }
                }

                updateState { it.copy(dialogDescriptor = alertDialogDescriptor) }
            }
            .launchIn(viewModelScope)

        createChatModel.findUserForGroupChat.jobFlow.asLceState()
            .onEach { state ->
                updateState { oldState ->
                    oldState.copy(
                        groupChat = oldState.groupChat.copy(
                            searchInProgress = state.isLoading,
                        ),
                    )
                }
            }
            .launchIn(viewModelScope)

        createChatModel.createGroupChat.jobFlow.asLceState()
            .onEach { state ->
                updateState { oldState ->
                    oldState.copy(
                        groupChat = oldState.groupChat.copy(
                            creationInProgress = state.isLoading,
                        ),
                    )
                }
            }
            .launchIn(viewModelScope)

        createChatModel.createGroupChat.jobFlow.asLceState()
            .map { it.toUiLceState(::createChatErrorMapper) }
            .onEach { state ->
                val alertDialogDescriptor = when (state) {
                    is UiLceState.Loading,
                    is UiLceState.NotStarted,
                    is UiLceState.Ready,
                        -> null

                    is UiLceState.Error -> {
                        val alertDialogContent = AlertDialogContent(
                            title = state.error.title,
                            message = state.error.description,
                            positiveButtonText = resPrintableText(StringRes.common_retry),
                            negativeButtonText = resPrintableText(StringRes.common_close),
                        )

                        AlertDialogDescriptor(
                            content = alertDialogContent,
                            positiveAction = { createChatModel.createGroupChat.startOnSubscribe() },
                            negativeAction = { updateState { it.copy(dialogDescriptor = null) } },
                            dismissAction = { updateState { it.copy(dialogDescriptor = null) } },
                        )
                    }
                }

                updateState { it.copy(dialogDescriptor = alertDialogDescriptor) }
            }
            .launchIn(viewModelScope)

        createChatModel.createGroupChat.jobFlow.successResults()
            .onEach { result ->
                sendEvent(AddChatEvent.GroupChatCreated(id = result.id))
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
                addedUsers = emptyList(),
                searchInProgress = false,
                creationInProgress = false,
            ),
            activeType = ChatType.Personal,
            dialogDescriptor = null,
        )
    }

    fun onUsernameChanged(newUsernameText: String) {
        when (currentViewState.activeType) {
            ChatType.Personal -> createChatModel.setPersonalChatUsername(newUsernameText)
            ChatType.Group -> createChatModel.setGroupChatUsername(newUsernameText)
        }
    }

    fun onFindClicked() {
        when (currentViewState.activeType) {
            ChatType.Personal -> createChatModel.createPersonalChat.startOnSubscribe()
            ChatType.Group -> createChatModel.findUserForGroupChat.startOnSubscribe()
        }
    }

    fun onTypeSelected(chatType: ChatType) {
        updateState { it.copy(activeType = chatType) }
    }

    fun onCreateGroupChatClicked() {
        createChatModel.createGroupChat.startOnSubscribe()
    }
}
