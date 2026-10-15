package com.security.chat.multiplatform.common.core.network

import com.security.chat.multiplatform.common.core.network.entity.SocketAuthEnvelope
import com.security.chat.multiplatform.common.core.network.entity.SocketAuthError
import com.security.chat.multiplatform.common.core.network.entity.SocketAuthState
import com.security.chat.multiplatform.common.core.network.entity.SocketAuthUpdate
import com.security.chat.multiplatform.common.log.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.uuid.Uuid

internal class SocketAuthorizationClient(
    private val tokenManager: TokenManager,
    private val json: Json,
    private val timeSource: TimeSource,
    private val send: suspend (SocketAuthUpdate) -> Unit,
    private val close: suspend () -> Unit,
) {
    private val replies = Channel<Reply>(8)

    suspend fun receive(payload: String) {
        val envelope = json.decodeFromString<SocketAuthEnvelope>(payload)
        when (envelope.type) {
            "auth.state", "auth.updated" -> replies.send(
                Reply.Lease(
                    envelope.type, json.decodeFromString(envelope.payload), timeSource.markNow(),
                ),
            )

            "auth.error" -> replies.send(Reply.Rejected(json.decodeFromString(envelope.payload)))
            else -> {
                Log.e("Unknown socket authorization event type: ${envelope.type}")
                throw AuthorizationRejected()
            }
        }
    }

    suspend fun run() {
        try {
            var lease = awaitLease(null)
            while (true) {
                delay((lease.remainingMillis() - REFRESH_LEAD_MS).coerceAtLeast(0L))
                while (true) {
                    val remaining = lease.remainingMillis()
                    if (remaining <= 0L) throw AuthorizationRejected()
                    try {
                        val tokens = withTimeout(remaining) {
                            tokenManager.refreshTokens()
                        } ?: throw AuthorizationRejected()
                        val remainingAfterRefresh = lease.remainingMillis()
                        if (remainingAfterRefresh <= 0L) throw AuthorizationRejected()
                        lease = withTimeout(minOf(REFRESH_TIMEOUT_MS, remainingAfterRefresh)) {
                            val requestId = Uuid.random().toString()
                            send(SocketAuthUpdate(requestId, tokens.accessToken.value))
                            awaitLease(requestId)
                        }
                        break
                    } catch (_: TimeoutCancellationException) {
                        Log.e("Socket authorization refresh timed out")
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: AuthorizationRejected) {
                        throw error
                    } catch (error: Throwable) {
                        Log.e("Socket authorization refresh failed: ${error::class.simpleName}")
                    }
                    delay(minOf(REFRESH_RETRY_MS, lease.remainingMillis().coerceAtLeast(0L)))
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Log.e("Socket authorization ended: ${error::class.simpleName}")
            close()
        }
    }

    private suspend fun awaitLease(requestId: String?): Reply.Lease {
        while (true) {
            when (val reply = replies.receive()) {
                is Reply.Lease -> {
                    val matches = if (requestId == null) reply.type == "auth.state" else
                        reply.type == "auth.updated" && reply.state.requestId == requestId
                    if (matches) {
                        if (reply.state.expiresInMillis <= 0L) throw AuthorizationRejected()
                        return reply
                    }
                }

                is Reply.Rejected -> {
                    if (reply.error.requestId == null || reply.error.requestId == requestId) {
                        throw AuthorizationRejected()
                    }
                }
            }
        }
    }

    private sealed interface Reply {
        data class Lease(val type: String, val state: SocketAuthState, val receivedAt: TimeMark) :
            Reply {
            fun remainingMillis(): Long =
                state.expiresInMillis - receivedAt.elapsedNow().inWholeMilliseconds
        }

        data class Rejected(val error: SocketAuthError) : Reply
    }

    private class AuthorizationRejected : Exception()
}

private const val REFRESH_LEAD_MS = 60_000L
private const val REFRESH_TIMEOUT_MS = 15_000L
private const val REFRESH_RETRY_MS = 5_000L
