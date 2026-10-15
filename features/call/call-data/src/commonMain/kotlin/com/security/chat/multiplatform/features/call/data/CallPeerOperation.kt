package com.security.chat.multiplatform.features.call.data

import com.security.chat.multiplatform.features.call.domain.entity.errors.CallMediaError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal suspend inline fun runPeerOperation(
    onFailure: (Throwable) -> Unit,
    block: () -> Unit,
) {
    try {
        block()
    } catch (error: Throwable) {
        /** An operation timeout is local; cancelling the call must still stop all its work. */
        currentCoroutineContext().ensureActive()
        if (error is CancellationException && error !is TimeoutCancellationException) throw error
        if (error !is Exception && error !is CallMediaError) throw error
        onFailure(error)
    }
}
