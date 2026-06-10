package com.security.chat.multiplatform.common.core.network

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

public interface LogoutErrorBroadcaster {
    public fun getLogoutFlow(): Flow<Unit>
}

public interface LogoutErrorAlerter {
    public fun logout()
}

internal class LogoutErrorBroadcasterImpl : LogoutErrorBroadcaster, LogoutErrorAlerter {

    private val logoutFlow = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override fun getLogoutFlow(): Flow<Unit> {
        return logoutFlow.asSharedFlow()
    }

    override fun logout() {
        logoutFlow.tryEmit(Unit)
    }
}