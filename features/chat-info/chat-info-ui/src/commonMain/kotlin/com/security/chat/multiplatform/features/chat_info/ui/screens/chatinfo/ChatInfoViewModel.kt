package com.security.chat.multiplatform.features.chat_info.ui.screens.chatinfo

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
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogContent
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor
import com.security.chat.multiplatform.features.chat_info.component.api.ChatInfoMainComponent
import com.security.chat.multiplatform.features.chat_info.domain.ChatInfoModel
import com.security.chat.multiplatform.features.chat_info.ui.screens.chatinfo.entity.ChatInfoUM
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import securitychat.common.localization.generated.resources.common_close
import securitychat.common.localization.generated.resources.common_retry

internal class ChatInfoViewModel(
    private val component: ChatInfoMainComponent,
    private val chatInfoModel: ChatInfoModel,
    private val dispatcherProviderInterface: DispatcherProviderInterface,
) : BaseViewModel<ChatInfoState, Unit>() {

    override fun createInitialState(): ChatInfoState = ChatInfoState(
        alertDialogDescriptor = null,
        chatInfoIsLoading = false,
        chatInfo = null,
    )

    override fun onPostStart() {
        super.onPostStart()

        chatInfoModel.setChatId(id = component.chatId)

        chatInfoModel.fetchInfo.jobFlow.asLceState().map { it.toUiLceState() }
            .onEach { loadingState ->
                when (loadingState) {
                    is UiLceState.Error -> {
                        val content = AlertDialogContent(
                            title = loadingState.error.title,
                            message = loadingState.error.description,
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
                                chatInfoModel.fetchInfo.startOnSubscribe()
                            },
                        )
                        updateState { it.copy(alertDialogDescriptor = alertDialogDescriptor) }
                    }

                    is UiLceState.Loading,
                    is UiLceState.NotStarted,
                    is UiLceState.Ready,
                        -> Unit
                }

                updateState { it.copy(chatInfoIsLoading = loadingState.isLoading) }
            }
            .flowOn(dispatcherProviderInterface.Default)
            .launchIn(viewModelScope)

        chatInfoModel.getChatInfoFlow()
            .filterNotNull()
            .onEach { info ->
                Log.d { "qwqweqwe info = $info" }

                val chatInfo = ChatInfoUM(
                    isAddMembersAllowed = info.isAddingMembersAllowed,
                )
                updateState { it.copy(chatInfo = chatInfo) }
            }
            .flowOn(dispatcherProviderInterface.Default)
            .launchIn(viewModelScope)

        chatInfoModel.fetchInfo.startOnSubscribe()
    }
}
