package com.security.chat.multiplatform.features.call.ui.screens.call

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.domain.asLceState
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import com.security.chat.multiplatform.common.core.ui.mappers.toUiLceState
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogContent
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor
import com.security.chat.multiplatform.features.call.component.api.CallMainComponent
import com.security.chat.multiplatform.features.call.domain.CallModel
import com.security.chat.multiplatform.features.call.ui.screens.call.entity.CallStatusUM
import com.security.chat.multiplatform.features.call.ui.screens.call.mapper.callErrorMapper
import com.security.chat.multiplatform.features.call.ui.screens.call.mapper.toUi
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import securitychat.common.localization.generated.resources.common_close

internal class CallViewModel(
    private val component: CallMainComponent,
    private val callModel: CallModel,
    private val dispatcherProvider: DispatcherProviderInterface,
) : BaseViewModel<CallState, CallEvent>() {

    override fun createInitialState(): CallState =
        CallState(
            onlineParticipants = persistentListOf(),
            callStatus = CallStatusUM.Idle,
            alertDialogDescriptor = null,
            isCallInProgress = false,
        )

    override fun onPostStart() {
        super.onPostStart()

        callModel.setChatId(component.chatId)

        viewActivable.activeFlow
            .onEach { callModel.setFocused(it) }
            .launchIn(viewModelScope)

        callModel.getPresenceFlow()
            .onEach { presence ->
                updateState { state ->
                    state.copy(
                        onlineParticipants = presence.participants.map { it.toUi() }
                            .toImmutableList(),
                        isCallInProgress = presence.isCallInProgress,
                    )
                }
            }
            .flowOn(dispatcherProvider.Default)
            .launchIn(viewModelScope)

        callModel.getCallStatusFlow()
            .onEach { status -> updateState { it.copy(callStatus = status.toUi()) } }
            .flowOn(dispatcherProvider.Default)
            .launchIn(viewModelScope)

        merge(
            callModel.startCall.jobFlow.asLceState(),
            callModel.endCall.jobFlow.asLceState(),
        ).map { it.toUiLceState(::callErrorMapper) }
            .onEach { result ->
                if (result is UiLceState.Error) {
                    val dismiss = { updateState { it.copy(alertDialogDescriptor = null) } }
                    updateState {
                        it.copy(
                            alertDialogDescriptor = AlertDialogDescriptor(
                                content = AlertDialogContent(
                                    title = result.error.title,
                                    message = result.error.description,
                                    positiveButtonText = resPrintableText(StringRes.common_close),
                                ),
                                dismissAction = dismiss,
                                positiveAction = dismiss,
                            ),
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onCallButtonClicked() {
        if (!currentViewState.isCallButtonEnabled) return

        when (currentViewState.callStatus) {
            CallStatusUM.Idle -> callModel.startCall.start()
            CallStatusUM.Connecting, CallStatusUM.Active -> callModel.endCall.start()
        }
    }
}
