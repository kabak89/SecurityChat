package com.security.chat.multiplatform.features.call.data

import com.security.chat.multiplatform.common.core.network.LiveEventsManager
import com.security.chat.multiplatform.common.core.network.entity.SocketEvent
import com.security.chat.multiplatform.features.call.data.entity.CallAcknowledgement
import com.security.chat.multiplatform.features.call.data.entity.CallCommand
import com.security.chat.multiplatform.features.call.data.entity.CallEnvelope
import com.security.chat.multiplatform.features.call.data.entity.CallErrorMessage
import com.security.chat.multiplatform.features.call.data.entity.FocusedSubscribeMessage
import com.security.chat.multiplatform.features.call.data.mapper.toCommand
import com.security.chat.multiplatform.features.call.data.mapper.toDomain
import com.security.chat.multiplatform.features.call.domain.entity.CallPeerConnection
import com.security.chat.multiplatform.features.call.domain.entity.CallRoomEvent
import com.security.chat.multiplatform.features.call.domain.entity.CallSignal
import com.security.chat.multiplatform.features.call.domain.entity.errors.CallSignalingError
import com.security.chat.multiplatform.features.call.domain.repo.CallSession
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal class CallSessionImpl(
    private val chatId: String,
    private val manager: LiveEventsManager,
    private val json: Json,
) : CallSession {
    private val connectionId = MutableStateFlow<Long?>(null)
    internal val transportConnectionId: Long? get() = connectionId.value

    private val pendingSignals = MutableStateFlow<Map<String, CallPeerConnection>>(emptyMap())

    override val events: Flow<CallRoomEvent> = channelFlow {
        launch(start = CoroutineStart.UNDISPATCHED) {
            var firstConnection: Long? = null
            manager.getConnectionIdFlow().collect { current ->
                if (firstConnection != null && current != firstConnection) {
                    throw CallSignalingError("DISCONNECTED")
                }
                if (current != null) firstConnection = current
            }
        }
        manager.subscribeEvents<CallEnvelope, FocusedSubscribeMessage>(
            FocusedSubscribeMessage(chatId), "call_events",
        ).collect { event ->
            when (event) {
                SocketEvent.Disconnected -> {
                    if (connectionId.value != null) throw CallSignalingError("DISCONNECTED")
                }

                is SocketEvent.Message -> {
                    connectionId.value = event.connectionId
                    decode(event.value)?.let { send(it) }
                }
            }
        }
    }

    override suspend fun join(requestId: String) {
        manager.send(CallCommand(requestId, chatId), "call.join", requireConnection())
    }

    override suspend fun leave(callId: String) {
        manager.send(
            CallCommand(Uuid.random().toString(), chatId, callId),
            "call.leave", requireConnection(),
        )
    }

    override suspend fun sendSignal(callId: String, signal: CallSignal) {
        val requestId = Uuid.random().toString()
        if (pendingSignals.value.size >= MAX_PENDING_SIGNALS) {
            throw CallSignalingError("SIGNAL_QUEUE_FULL")
        }
        pendingSignals.update { it + (requestId to signal.connection) }
        try {
            manager.send(
                signal.toCommand(chatId, callId, requestId),
                "call.signal", requireConnection(),
            )
        } catch (error: Throwable) {
            pendingSignals.update { it - requestId }
            throw error
        }
    }

    private fun decode(envelope: CallEnvelope): CallRoomEvent? {
        if (envelope.type == "call.ack") {
            val ack = json.decodeFromString<CallAcknowledgement>(envelope.payload)
            pendingSignals.update { it - ack.requestId }
            return null
        }
        if (envelope.type == "call.error") {
            val error = json.decodeFromString<CallErrorMessage>(envelope.payload)
            val connection = pendingSignals.value[error.requestId]
            if (connection != null) {
                pendingSignals.update { it - requireNotNull(error.requestId) }
                return CallRoomEvent.SignalRejected(connection, error.code)
            }
        }
        return envelope.toDomain(chatId, json)
    }

    private fun requireConnection(): Long =
        connectionId.value ?: throw CallSignalingError("NOT_CONNECTED")
}

private const val MAX_PENDING_SIGNALS = 256
