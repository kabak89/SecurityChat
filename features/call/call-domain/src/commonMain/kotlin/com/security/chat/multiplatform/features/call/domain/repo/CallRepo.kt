package com.security.chat.multiplatform.features.call.domain.repo

import com.security.chat.multiplatform.features.call.domain.entity.CallPresence
import com.security.chat.multiplatform.features.call.domain.entity.CallRoomEvent
import com.security.chat.multiplatform.features.call.domain.entity.CallSignal
import kotlinx.coroutines.flow.Flow

public interface CallRepo {
    public fun getPresenceFlow(chatId: String): Flow<CallPresence>

    /** Uses the room subscription maintained by the active presence observer. */
    public suspend fun withCallSession(chatId: String, block: suspend (CallSession) -> Unit)
}

public interface CallSession {
    /** Each audio attempt collects once; termination or transport replacement fails that call. */
    public val events: Flow<CallRoomEvent>
    public suspend fun join(requestId: String)
    public suspend fun leave(callId: String)
    public suspend fun sendSignal(callId: String, signal: CallSignal)
}
