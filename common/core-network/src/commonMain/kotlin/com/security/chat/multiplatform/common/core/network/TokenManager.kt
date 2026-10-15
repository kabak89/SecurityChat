package com.security.chat.multiplatform.common.core.network

import com.security.chat.multiplatform.common.core.error.NetworkError
import com.security.chat.multiplatform.common.core.network.entity.AccessToken
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.common.core.network.entity.RefreshRequest
import com.security.chat.multiplatform.common.core.network.entity.RefreshResponse
import com.security.chat.multiplatform.common.core.network.entity.RefreshToken
import com.security.chat.multiplatform.common.core.network.entity.Tokens
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.common.settings.EncryptedSettings
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid

public interface TokenManager {
    public suspend fun saveTokens(tokens: Tokens)
    public suspend fun getTokens(): Tokens?
    public suspend fun refreshTokens(): Tokens?
    public suspend fun clearTokens()
}

internal class TokenManagerImpl(
    private val encryptedSettings: EncryptedSettings,
    private val logoutErrorAlerter: LogoutErrorAlerter,
    private val dispatcherProviderInterface: DispatcherProviderInterface,
    private val networkManagerFactory: NetworkManagerFactory,
    private val networkConfig: NetworkConfig,
    private val coroutineScope: CoroutineScope,
    private val json: Json,
) : TokenManager {
    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(
            baseUrl = "${networkConfig.host}:${networkConfig.port}",
            needAuthorization = false,
        )
    }
    private val stateMutex = Mutex()
    private var refresh: Deferred<Tokens?>? = null
    private var generation = 0L

    override suspend fun saveTokens(tokens: Tokens) {
        withContext(NonCancellable + dispatcherProviderInterface.IO) {
            stateMutex.withLock {
                writeTokens(tokens)
                encryptedSettings.putString(KEY_PENDING_REFRESH, null)
                generation++
                refresh = null
            }
        }
    }

    override suspend fun getTokens(): Tokens? =
        withContext(dispatcherProviderInterface.IO) {
            stateMutex.withLock {
                readTokens().also { tokens ->
                    if (tokens == null) {
                        Log.e("No tokens in storage")
                        logoutErrorAlerter.logout()
                    }
                }
            }
        }

    override suspend fun refreshTokens(): Tokens? {
        val operation = withContext(dispatcherProviderInterface.IO) {
            stateMutex.withLock {
                refresh?.takeUnless { it.isCompleted } ?: run {
                    val tokens = readTokens() ?: return@withLock null
                    val request = encryptedSettings.getString(KEY_PENDING_REFRESH)
                        ?.let { json.decodeFromString<RefreshRequest>(it) }
                        ?.takeIf { it.refreshToken == tokens.refreshToken.value }
                        ?: RefreshRequest(tokens.refreshToken.value, Uuid.random().toString())
                    encryptedSettings.putString(KEY_PENDING_REFRESH, json.encodeToString(request))
                    val currentGeneration = generation
                    /** Cancelling a waiter cannot cancel the app-owned rotation. */
                    coroutineScope.async {
                        performRefresh(request, currentGeneration)
                    }.also { refresh = it }
                }
            }
        } ?: return null
        return operation.await()
    }

    override suspend fun clearTokens() {
        withContext(NonCancellable + dispatcherProviderInterface.IO) {
            stateMutex.withLock {
                encryptedSettings.putString(KEY_TOKENS, null)
                encryptedSettings.putString(KEY_ACCESS_TOKEN, null)
                encryptedSettings.putString(KEY_REFRESH_TOKEN, null)
                encryptedSettings.putString(KEY_PENDING_REFRESH, null)
                generation++
                refresh = null
            }
        }
    }

    private suspend fun performRefresh(request: RefreshRequest, requestGeneration: Long): Tokens? {
        repeat(REFRESH_ATTEMPTS) { attempt ->
            if (!isCurrent(request, requestGeneration)) return null
            val response = try {
                withTimeout(REFRESH_REQUEST_TIMEOUT_MS) {
                    networkManager.runPost<RefreshRequest, RefreshResponse>("/refresh", request)
                }
            } catch (error: Throwable) {
                if (error is CancellationException && error !is TimeoutCancellationException) {
                    throw error
                }
                if (!isCurrent(request, requestGeneration)) return null
                if (error is NetworkError &&
                    error.statusCode == HttpStatusCode.Unauthorized.value
                ) {
                    withContext(dispatcherProviderInterface.IO) {
                        stateMutex.withLock {
                            if (isCurrentLocked(request, requestGeneration)) {
                                encryptedSettings.putString(KEY_PENDING_REFRESH, null)
                                logoutErrorAlerter.logout()
                            }
                        }
                    }
                    return null
                }
                val isClientError = error is NetworkError && error.statusCode in 400..499 &&
                        error.statusCode != 408 && error.statusCode != 429
                if (isClientError || attempt == REFRESH_ATTEMPTS - 1) throw error
                Log.e("Token refresh failed: ${error::class.simpleName}; retrying")
                delay(REFRESH_RETRY_MS)
                return@repeat
            }
            return withContext(NonCancellable + dispatcherProviderInterface.IO) {
                stateMutex.withLock {
                    if (!isCurrentLocked(request, requestGeneration)) return@withLock null
                    val tokens = Tokens(
                        AccessToken(response.accessToken), RefreshToken(response.refreshToken),
                    )
                    writeTokens(tokens)
                    encryptedSettings.putString(KEY_PENDING_REFRESH, null)
                    tokens
                }
            }
        }
        error("Refresh attempts exhausted")
    }

    private suspend fun isCurrent(request: RefreshRequest, requestGeneration: Long): Boolean =
        withContext(dispatcherProviderInterface.IO) {
            stateMutex.withLock { isCurrentLocked(request, requestGeneration) }
        }

    private fun isCurrentLocked(request: RefreshRequest, requestGeneration: Long): Boolean =
        generation == requestGeneration && readTokens()?.refreshToken?.value == request.refreshToken

    private fun readTokens(): Tokens? {
        val stored = encryptedSettings.getString(KEY_TOKENS)
        if (stored != null) {
            val tokens = json.decodeFromString<StoredTokens>(stored)
            return Tokens(AccessToken(tokens.accessToken), RefreshToken(tokens.refreshToken))
        }
        val accessToken = encryptedSettings.getString(KEY_ACCESS_TOKEN) ?: return null
        val refreshToken = encryptedSettings.getString(KEY_REFRESH_TOKEN) ?: return null
        return Tokens(AccessToken(accessToken), RefreshToken(refreshToken)).also { writeTokens(it) }
    }

    private fun writeTokens(tokens: Tokens) {
        /** Store the pair in one value so readers and app restarts cannot see half a rotation. */
        encryptedSettings.putString(
            KEY_TOKENS,
            json.encodeToString(
                StoredTokens(tokens.accessToken.value, tokens.refreshToken.value),
            ),
        )
        encryptedSettings.putString(KEY_ACCESS_TOKEN, null)
        encryptedSettings.putString(KEY_REFRESH_TOKEN, null)
    }

    @Serializable
    private data class StoredTokens(val accessToken: String, val refreshToken: String)
}

private const val KEY_TOKENS = "KEY_TOKENS"
private const val KEY_ACCESS_TOKEN = "KEY_ACCESS_TOKEN"
private const val KEY_REFRESH_TOKEN = "KEY_REFRESH_TOKEN"
private const val KEY_PENDING_REFRESH = "KEY_PENDING_REFRESH"
private const val REFRESH_REQUEST_TIMEOUT_MS = 30_000L
private const val REFRESH_RETRY_MS = 5_000L
private const val REFRESH_ATTEMPTS = 3
