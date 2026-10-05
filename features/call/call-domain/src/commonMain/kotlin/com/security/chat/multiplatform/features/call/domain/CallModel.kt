package com.security.chat.multiplatform.features.call.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.call.domain.entity.CallParticipant
import com.security.chat.multiplatform.features.call.domain.entity.CallStatus
import com.security.chat.multiplatform.features.call.domain.repo.CallRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

public interface CallModel : ScopedModel {

    public fun setChatId(id: String)
    public fun setFocused(isFocused: Boolean)
    public fun getFocusedParticipantsFlow(): Flow<List<CallParticipant>>
    public fun getCallStatusFlow(): Flow<CallStatus>
    public fun startCall()
    public fun endCall()
}

internal class CallModelImpl(
    private val callRepo: CallRepo,
    dispatcherProvider: DispatcherProviderInterface,
) : CallModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
    ) {

    private val stateFlow = MutableStateFlow(State())

    override fun setChatId(id: String) {
        check(stateFlow.value.chatId == null)
        stateFlow.update { it.copy(chatId = id) }
    }

    override fun setFocused(isFocused: Boolean) {
        stateFlow.update { it.copy(isFocused = isFocused) }
    }

    override fun getFocusedParticipantsFlow(): Flow<List<CallParticipant>> {
        return stateFlow
            .map { it.chatId to it.isFocused }
            .distinctUntilChanged()
            .flatMapLatest { (chatId, isFocused) ->
                if (chatId != null && isFocused) {
                    callRepo.getFocusedParticipantsFlow(chatId)
                } else {
                    flowOf(emptyList())
                }
            }
    }

    override fun getCallStatusFlow(): Flow<CallStatus> {
        return stateFlow.map { it.callStatus }.distinctUntilChanged()
    }

    override fun startCall() {
        /** TODO: Establish WebRTC connections and update the call status. */
    }

    override fun endCall() {
        /** TODO: Close WebRTC connections and reset the call status. */
    }

    private data class State(
        val chatId: String? = null,
        val isFocused: Boolean = false,
        val callStatus: CallStatus = CallStatus.Idle,
    )
}
