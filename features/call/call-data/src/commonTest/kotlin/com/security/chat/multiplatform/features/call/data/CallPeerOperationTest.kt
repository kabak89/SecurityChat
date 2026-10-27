package com.security.chat.multiplatform.features.call.data

import com.security.chat.multiplatform.features.call.domain.entity.errors.CallMediaError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CallPeerOperationTest {
    @Test
    fun `media and native errors fail only their peer operation`() = runTest {
        val failures = mutableListOf<Throwable>()
        val errors = listOf(
            CallMediaError.Connection("SDP failed"),
            IllegalArgumentException("Invalid ICE"),
        )
        for (error in errors) {
            runPeerOperation(onFailure = { failures += it }) { throw error }
        }
        var anotherPeerContinued = false
        runPeerOperation(onFailure = { failures += it }) { anotherPeerContinued = true }
        assertEquals(errors, failures)
        assertTrue(anotherPeerContinued)
    }

    @Test
    fun `SDP operation timeout does not cancel the call coroutine`() = runTest {
        val failures = mutableListOf<Throwable>()
        runPeerOperation(onFailure = { failures += it }) {
            withTimeout(100L) { awaitCancellation() }
        }
        assertIs<TimeoutCancellationException>(failures.single())
        var continued = false
        runPeerOperation(onFailure = { failures += it }) { continued = true }
        assertTrue(continued)
        assertEquals(1, failures.size)
    }

    @Test
    fun `external cancellation interrupts a pending peer operation`() = runTest {
        val entered = CompletableDeferred<Unit>()
        val failures = mutableListOf<Throwable>()
        val job = backgroundScope.launch {
            runPeerOperation(onFailure = { failures += it }) {
                entered.complete(Unit)
                awaitCancellation()
            }
        }
        entered.await()
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertTrue(failures.isEmpty())
    }

    @Test
    fun `explicit cancellation and fatal errors are not converted into peer failures`() = runTest {
        val failures = mutableListOf<Throwable>()
        assertFailsWith<CancellationException> {
            runPeerOperation(onFailure = { failures += it }) {
                throw CancellationException("Call ended")
            }
        }
        assertFailsWith<AssertionError> {
            runPeerOperation(onFailure = { failures += it }) { throw AssertionError("Fatal error") }
        }
        assertTrue(failures.isEmpty())
    }
}
