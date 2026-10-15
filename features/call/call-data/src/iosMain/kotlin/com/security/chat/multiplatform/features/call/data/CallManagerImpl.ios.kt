package com.security.chat.multiplatform.features.call.data

import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.call.domain.CallManager
import com.security.chat.multiplatform.features.call.domain.entity.CallIceServer
import com.security.chat.multiplatform.features.call.domain.entity.CallManagerState
import com.security.chat.multiplatform.features.call.domain.entity.CallPeerConnection
import com.security.chat.multiplatform.features.call.domain.entity.CallSignal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

@Suppress("UNUSED_PARAMETER")
internal actual class CallManagerImpl actual constructor(
    dispatcherProvider: DispatcherProviderInterface,
) : CallManager {

    private val state = MutableStateFlow(CallManagerState())

    override fun getStateFlow(): StateFlow<CallManagerState> = state.asStateFlow()

    override fun getOutgoingSignalsFlow(): Flow<CallSignal> = emptyFlow()

    override suspend fun start(iceServers: List<CallIceServer>) {
        TODO("Implement WebRTC audio on iOS")
    }

    override suspend fun connect(connection: CallPeerConnection, isInitiator: Boolean) {
        TODO("Implement WebRTC peer connections on iOS")
    }

    override suspend fun handleSignal(signal: CallSignal) {
        TODO("Implement WebRTC signaling on iOS")
    }

    override suspend fun failConnection(connection: CallPeerConnection, error: Throwable) {
        TODO("Implement WebRTC connection failure handling on iOS")
    }

    override suspend fun disconnect(connection: CallPeerConnection) {
        TODO("Implement WebRTC disconnection on iOS")
    }

    override suspend fun stop() {
        /** TODO: Release iOS WebRTC resources when the implementation is added. */
        state.value = CallManagerState()
    }
}
