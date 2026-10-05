package com.security.chat.multiplatform.features.call.ui.screens.call

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.features.call.component.api.CallMainComponent
import com.security.chat.multiplatform.features.call.domain.CallModel
import com.security.chat.multiplatform.features.call.ui.screens.call.entity.CallStatusUM
import com.security.chat.multiplatform.features.call.ui.screens.call.mapper.toUi
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class CallViewModel(
    private val component: CallMainComponent,
    private val callModel: CallModel,
    private val dispatcherProvider: DispatcherProviderInterface,
) : BaseViewModel<CallState, CallEvent>() {

    override fun createInitialState(): CallState =
        CallState(
            onlineParticipants = persistentListOf(),
            callStatus = CallStatusUM.Idle,
        )

    override fun onPostStart() {
        super.onPostStart()

        callModel.setChatId(component.chatId)

        viewActivable.activeFlow
            .onEach { callModel.setFocused(it) }
            .launchIn(viewModelScope)

        callModel.getFocusedParticipantsFlow()
            .onEach { participants ->
                updateState { state ->
                    state.copy(
                        onlineParticipants = participants.map { it.toUi() }
                            .toImmutableList(),
                    )
                }
            }
            .flowOn(dispatcherProvider.Default)
            .launchIn(viewModelScope)

        callModel.getCallStatusFlow()
            .onEach { status -> updateState { it.copy(callStatus = status.toUi()) } }
            .flowOn(dispatcherProvider.Default)
            .launchIn(viewModelScope)
    }

    fun onCallButtonClicked() {
        if (!currentViewState.isCallButtonEnabled) return

        when (currentViewState.callStatus) {
            CallStatusUM.Idle -> callModel.startCall()
            CallStatusUM.Connecting -> Unit
            CallStatusUM.Active -> callModel.endCall()
        }
    }
}
