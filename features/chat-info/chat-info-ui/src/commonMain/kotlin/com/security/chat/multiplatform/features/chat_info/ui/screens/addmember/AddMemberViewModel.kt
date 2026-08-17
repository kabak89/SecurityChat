package com.security.chat.multiplatform.features.chat_info.ui.screens.addmember

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.domain.asLceState
import com.security.chat.multiplatform.common.core.domain.startOnSubscribe
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState
import com.security.chat.multiplatform.common.core.ui.entity.isLoading
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import com.security.chat.multiplatform.common.core.ui.mappers.toUiLceState
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogContent
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor
import com.security.chat.multiplatform.features.chat_info.component.api.AddMemberComponent
import com.security.chat.multiplatform.features.chat_info.domain.AddMemberModel
import com.security.chat.multiplatform.features.chat_info.ui.screens.addmember.mapper.createAddMemberErrorMapper
import com.security.chat.multiplatform.features.chat_info.ui.screens.addmember.mapper.isUserAlreadyInChatError
import com.security.chat.multiplatform.features.chat_info.ui.screens.addmember.mapper.isUserAlreadyInListError
import com.security.chat.multiplatform.features.chat_info.ui.screens.addmember.mapper.toUi
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ru.kode.remo.successResults
import securitychat.common.localization.generated.resources.common_close
import securitychat.common.localization.generated.resources.common_retry

internal class AddMemberViewModel(
    private val component: AddMemberComponent,
    private val addMemberModel: AddMemberModel,
    private val dispatcherProviderInterface: DispatcherProviderInterface,
) : BaseViewModel<AddMemberState, Unit>() {

    override fun onPostStart() {
        super.onPostStart()

        addMemberModel.setChatId(id = component.chatId)

        addMemberModel.fetchChatInfo.jobFlow.asLceState().map { it.toUiLceState() }
            .onEach { fetchState ->
                if (fetchState is UiLceState.Error) {
                    val content = AlertDialogContent(
                        title = fetchState.error.title,
                        message = fetchState.error.description,
                        positiveButtonText = resPrintableText(StringRes.common_close),
                        negativeButtonText = resPrintableText(StringRes.common_retry),
                    )
                    val alertDialogDescriptor = AlertDialogDescriptor(
                        content = content,
                        dismissAction = {
                            updateState { it.copy(alertDialogDescriptor = null) }
                        },
                        positiveAction = {
                            updateState { it.copy(alertDialogDescriptor = null) }
                        },
                        negativeAction = {
                            updateState { it.copy(alertDialogDescriptor = null) }
                            addMemberModel.fetchChatInfo.startOnSubscribe()
                        },
                    )
                    updateState { it.copy(alertDialogDescriptor = alertDialogDescriptor) }
                }
            }
            .flowOn(dispatcherProviderInterface.Default)
            .launchIn(viewModelScope)

        addMemberModel.fetchChatInfo.startOnSubscribe()

        addMemberModel.getStateFlow()
            .onEach { domainState ->
                updateState { oldState ->
                    oldState.copy(
                        username = domainState.memberNameForSearch,
                        foundMembers = domainState.foundMembers.map { it.toUi() },
                    )
                }
            }
            .launchIn(viewModelScope)

        addMemberModel.search.jobFlow.asLceState()
            .map { it.toUiLceState(::createAddMemberErrorMapper) }
            .onEach { searchState ->
                updateState { it.copy(searchInProgress = searchState.isLoading) }

                if (searchState is UiLceState.Error) {
                    val cause = searchState.error.cause

                    val alertDialogDescriptor = when {
                        cause.isUserAlreadyInChatError() || cause.isUserAlreadyInListError() -> {
                            val content = AlertDialogContent(
                                title = searchState.error.title,
                                message = searchState.error.description,
                                positiveButtonText = resPrintableText(StringRes.common_close),
                            )
                            AlertDialogDescriptor(
                                content = content,
                                dismissAction = {
                                    updateState { it.copy(alertDialogDescriptor = null) }
                                },
                                positiveAction = {
                                    updateState { it.copy(alertDialogDescriptor = null) }
                                },
                            )
                        }

                        else -> {
                            val content = AlertDialogContent(
                                title = searchState.error.title,
                                message = searchState.error.description,
                                positiveButtonText = resPrintableText(StringRes.common_close),
                                negativeButtonText = resPrintableText(StringRes.common_retry),
                            )
                            AlertDialogDescriptor(
                                content = content,
                                dismissAction = {
                                    updateState { it.copy(alertDialogDescriptor = null) }
                                },
                                positiveAction = {
                                    updateState { it.copy(alertDialogDescriptor = null) }
                                },
                                negativeAction = {
                                    updateState { it.copy(alertDialogDescriptor = null) }
                                    addMemberModel.search.startOnSubscribe()
                                },
                            )
                        }
                    }
                    updateState { it.copy(alertDialogDescriptor = alertDialogDescriptor) }
                }
            }
            .flowOn(dispatcherProviderInterface.Default)
            .launchIn(viewModelScope)

        addMemberModel.addMembers.jobFlow.asLceState()
            .map { it.toUiLceState(::createAddMemberErrorMapper) }
            .onEach { addingState ->
                updateState { it.copy(addingInProgress = addingState.isLoading) }

                if (addingState is UiLceState.Error) {
                    val content = AlertDialogContent(
                        title = addingState.error.title,
                        message = addingState.error.description,
                        positiveButtonText = resPrintableText(StringRes.common_close),
                        negativeButtonText = resPrintableText(StringRes.common_retry),
                    )
                    val alertDialogDescriptor = AlertDialogDescriptor(
                        content = content,
                        dismissAction = {
                            updateState { it.copy(alertDialogDescriptor = null) }
                        },
                        positiveAction = {
                            updateState { it.copy(alertDialogDescriptor = null) }
                        },
                        negativeAction = {
                            updateState { it.copy(alertDialogDescriptor = null) }
                            addMemberModel.addMembers.startOnSubscribe()
                        },
                    )
                    updateState { it.copy(alertDialogDescriptor = alertDialogDescriptor) }
                }
            }
            .flowOn(dispatcherProviderInterface.Default)
            .launchIn(viewModelScope)

        addMemberModel.addMembers.jobFlow.successResults()
            .onEach {
                component.onMembersAdded()
            }
            .flowOn(dispatcherProviderInterface.Default)
            .launchIn(viewModelScope)
    }

    override fun createInitialState(): AddMemberState = AddMemberState(
        username = "",
        searchInProgress = false,
        addingInProgress = false,
        foundMembers = emptyList(),
        alertDialogDescriptor = null,
    )

    fun onUsernameTextChanged(username: String) {
        addMemberModel.setMemberName(username = username)
    }

    fun onFindClicked() {
        addMemberModel.search.startOnSubscribe()
    }

    fun onAddClicked() {
        addMemberModel.addMembers.startOnSubscribe()
    }

    fun onRemoveMemberClicked(memberId: String) {
        addMemberModel.removeMember(memberId)
    }
}
