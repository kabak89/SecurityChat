package com.security.chat.multiplatform.common.core.network

import com.security.chat.multiplatform.common.core.error.NetworkError
import com.security.chat.multiplatform.common.core.network.entity.AccessToken
import com.security.chat.multiplatform.common.core.network.entity.RefreshToken
import com.security.chat.multiplatform.common.core.network.entity.SocketAuthEnvelope
import com.security.chat.multiplatform.common.core.network.entity.SocketAuthError
import com.security.chat.multiplatform.common.core.network.entity.SocketAuthState
import com.security.chat.multiplatform.common.core.network.entity.SocketAuthUpdate
import com.security.chat.multiplatform.common.core.network.entity.Tokens
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SocketAuthorizationClientTest {
    @Test
    fun `pending token refresh closes the socket at the confirmed lease deadline`() = runTest {
        val f = Fixture(this)
        f.tokens.refreshDelay = 100_000L
        f.state(120_000L)
        runCurrent()
        advanceTimeBy(60_000L)
        runCurrent()
        assertEquals(1, f.tokens.refreshCalls)
        advanceTimeBy(59_999L)
        runCurrent()
        assertFalse(f.closed)
        advanceTimeBy(1L)
        runCurrent()
        assertTrue(f.closed)
        assertTrue(f.job.isCompleted)
        assertTrue(f.requests.tryReceive().isFailure)
        advanceTimeBy(40_001L)
        runCurrent()
        assertEquals(1, f.tokens.refreshCalls)
        assertTrue(f.requests.tryReceive().isFailure)
    }

    @Test
    fun `slow token refresh does not use the socket acknowledgment timeout`() = runTest {
        val f = Fixture(this)
        f.tokens.refreshDelay = 20_000L
        f.state(120_000L)
        runCurrent()
        advanceTimeBy(80_001L)
        runCurrent()
        val update = f.requests.receive()
        assertEquals(1, f.tokens.refreshCalls)
        assertFalse(f.closed)
        f.state(120_000L, update.requestId)
        runCurrent()
        assertFalse(f.closed)
    }

    @Test
    fun `authorization renews one minute early and continues beyond the original deadline`() =
        runTest {
            val f = Fixture(this)
            f.state(120_000L)
            runCurrent()
            advanceTimeBy(59_999L)
            runCurrent()
            assertEquals(0, f.tokens.refreshCalls)
            advanceTimeBy(1L)
            runCurrent()
            val first = f.requests.receive()
            assertEquals("renewed-access", first.accessToken)
            f.state(120_000L, first.requestId)
            runCurrent()
            advanceTimeBy(60_000L)
            runCurrent()
            assertEquals(2, f.tokens.refreshCalls)
            val second = f.requests.receive()
            assertTrue(first.requestId != second.requestId)
            f.state(120_000L, second.requestId)
            runCurrent()
            assertFalse(f.closed)
        }

    @Test
    fun `temporary refresh failure retries before the confirmed lease expires`() = runTest {
        val f = Fixture(this)
        f.tokens.error = NetworkError(503)
        f.state(90_000L)
        runCurrent()
        advanceTimeBy(30_000L)
        runCurrent()
        assertEquals(1, f.tokens.refreshCalls)
        assertFalse(f.closed)
        f.tokens.error = null
        advanceTimeBy(5_000L)
        runCurrent()
        val update = f.requests.receive()
        f.state(120_000L, update.requestId)
        runCurrent()
        assertEquals(2, f.tokens.refreshCalls)
        assertFalse(f.closed)
    }

    @Test
    fun `missing acknowledgment closes the socket at the original deadline`() = runTest {
        val f = Fixture(this)
        f.state(20_000L)
        runCurrent()
        f.requests.receive()
        advanceTimeBy(20_001L)
        runCurrent()
        assertTrue(f.closed)
        assertEquals(1, f.tokens.refreshCalls)
    }

    @Test
    fun `an acknowledgment for another request cannot extend authorization`() = runTest {
        val f = Fixture(this)
        f.state(20_000L)
        runCurrent()
        f.requests.receive()
        f.state(120_000L, "another-request")
        advanceTimeBy(20_001L)
        runCurrent()
        assertTrue(f.closed)
    }

    @Test
    fun `missing tokens and rejected authorization close the socket`() = runTest {
        val missing = Fixture(this)
        missing.tokens.tokens = null
        missing.state(20_000L)
        runCurrent()
        assertTrue(missing.closed)
        assertTrue(missing.requests.tryReceive().isFailure)
        val rejected = Fixture(this)
        rejected.state(20_000L)
        runCurrent()
        val request = rejected.requests.receive()
        rejected.worker.receive(
            Json.encodeToString(
                SocketAuthEnvelope(
                    "auth.error",
                    Json.encodeToString(SocketAuthError("IDENTITY_MISMATCH", request.requestId)),
                ),
            ),
        )
        runCurrent()
        assertTrue(rejected.closed)
    }

    @Test
    fun `ending the socket cancels its scheduled authorization refresh`() = runTest {
        val f = Fixture(this)
        f.state(120_000L)
        runCurrent()
        f.job.cancelAndJoin()
        advanceTimeBy(60_001L)
        runCurrent()
        assertEquals(0, f.tokens.refreshCalls)
        assertTrue(f.requests.tryReceive().isFailure)
        assertFalse(f.closed)
    }

    private class Fixture(scope: TestScope) {
        val tokens = FakeTokens()
        val requests = Channel<SocketAuthUpdate>(Channel.UNLIMITED)
        var closed = false
        val worker = SocketAuthorizationClient(
            tokens, Json, scope.testScheduler.timeSource,
            send = { requests.send(it) }, close = { closed = true },
        )
        val job = scope.backgroundScope.launch { worker.run() }

        suspend fun state(expiresInMillis: Long, requestId: String? = null) {
            val type = if (requestId == null) "auth.state" else "auth.updated"
            worker.receive(
                Json.encodeToString(
                    SocketAuthEnvelope(
                        type,
                        Json.encodeToString(SocketAuthState(1L, expiresInMillis, requestId)),
                    ),
                ),
            )
        }
    }

    private class FakeTokens : TokenManager {
        var tokens: Tokens? = Tokens(AccessToken("renewed-access"), RefreshToken("refresh"))
        var refreshCalls = 0
        var refreshDelay = 0L
        var error: Throwable? = null
        override suspend fun saveTokens(tokens: Tokens) {
            this.tokens = tokens
        }

        override suspend fun getTokens(): Tokens? = tokens
        override suspend fun refreshTokens(): Tokens? {
            refreshCalls++
            delay(refreshDelay)
            error?.let { throw it }
            return tokens
        }

        override suspend fun clearTokens() {
            tokens = null
        }
    }
}
