package com.security.chat.multiplatform.features.call.data

import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.call.domain.entity.CallRoomEvent
import com.security.chat.multiplatform.features.call.domain.entity.CallRoomSnapshot
import com.security.chat.multiplatform.features.call.domain.entity.errors.CallSignalingError
import com.security.chat.multiplatform.features.call.domain.repo.CallSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal class ObservedCallSession(
    val chatId: String,
    private val session: CallSessionImpl,
) : CallSession by session {
    val snapshot = MutableStateFlow<CallRoomSnapshot?>(null)
    val connectionId: Long? get() = session.transportConnectionId
    private val incoming = MutableSharedFlow<Result<CallRoomEvent>>()
    private val isInUse = MutableStateFlow(false)
    private val failure = MutableStateFlow<Throwable?>(null)

    override val events: Flow<CallRoomEvent> = incoming
        .onSubscription {
            val error = failure.value
            if (error != null) {
                emit(Result.failure(error))
            } else {
                snapshot.value?.let { emit(Result.success(CallRoomEvent.Snapshot(it))) }
            }
        }
        .map { it.getOrThrow() }

    suspend fun observe() {
        try {
            session.events.collect { event ->
                when (event) {
                    is CallRoomEvent.Snapshot -> snapshot.value = event.snapshot
                    is CallRoomEvent.Joined -> snapshot.value = event.snapshot
                    else -> Unit
                }
                if (event is CallRoomEvent.Error) {
                    incoming.emit(Result.failure(CallSignalingError(event.code, event.message)))
                } else {
                    incoming.emit(Result.success(event))
                }
                if (!isInUse.value && event is CallRoomEvent.Joined) leaveIfJoined()
            }
            throw CallSignalingError("DISCONNECTED")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            failure.value = error
            incoming.emit(Result.failure(error))
            throw error
        }
    }

    suspend fun use(block: suspend (CallSession) -> Unit) {
        failure.value?.let { throw it }
        check(isInUse.compareAndSet(false, true)) { "Call session is already in use" }
        try {
            block(this)
        } finally {
            withContext(NonCancellable) {
                isInUse.value = false
                try {
                    leaveIfJoined()
                } catch (error: Throwable) {
                    Log.e(error, "Could not leave observed call session")
                }
            }
        }
    }

    private suspend fun leaveIfJoined() {
        val current = snapshot.value ?: return
        if (current.participantSessionId == null) return
        withTimeoutOrNull(2_000L) { session.leave(requireNotNull(current.callId)) }
    }
}
