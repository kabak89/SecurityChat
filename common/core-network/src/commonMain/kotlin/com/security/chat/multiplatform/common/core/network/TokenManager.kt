package com.security.chat.multiplatform.common.core.network

import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.mapBoth
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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
) : TokenManager {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(
            baseUrl = "${networkConfig.host}:${networkConfig.port}",
            needAuthorization = false,
        )
    }

    private val refreshMutex = Mutex()

    override suspend fun saveTokens(tokens: Tokens) {
        withContext(dispatcherProviderInterface.IO) {
            encryptedSettings.putString(KEY_ACCESS_TOKEN, tokens.accessToken.value)
            encryptedSettings.putString(KEY_REFRESH_TOKEN, tokens.refreshToken.value)
        }
    }

    override suspend fun getTokens(): Tokens? {
        return getTokensInternal()
    }

    override suspend fun refreshTokens(): Tokens? {
        val oldRefreshToken = withContext(dispatcherProviderInterface.IO) {
            encryptedSettings.getString(KEY_REFRESH_TOKEN)
        } ?: return null

        return refreshMutex.withLock {
            val currentRefreshToken = withContext(dispatcherProviderInterface.IO) {
                encryptedSettings.getString(KEY_REFRESH_TOKEN)
            }
            if (currentRefreshToken != null && currentRefreshToken != oldRefreshToken) {
                return@withLock getTokens()
            }

            performRefresh(oldRefreshToken)
        }
    }

    override suspend fun clearTokens() {
        withContext(dispatcherProviderInterface.IO) {
            encryptedSettings.putString(KEY_ACCESS_TOKEN, null)
            encryptedSettings.putString(KEY_REFRESH_TOKEN, null)
        }
    }

    private suspend fun performRefresh(refreshToken: String): Tokens? {
        return runSuspendCatching {
            networkManager.runPost<RefreshRequest, RefreshResponse>(
                relativePath = "/refresh",
                request = RefreshRequest(
                    refreshToken = refreshToken,
                ),
            )
        }
            .mapBoth(
                success = { response ->
                    val newAccessToken = response.accessToken
                    val newRefreshToken = response.refreshToken

                    withContext(dispatcherProviderInterface.IO) {
                        encryptedSettings.putString(KEY_ACCESS_TOKEN, newAccessToken)
                        encryptedSettings.putString(KEY_REFRESH_TOKEN, newRefreshToken)
                    }

                    Tokens(
                        accessToken = AccessToken(newAccessToken),
                        refreshToken = RefreshToken(newRefreshToken),
                    )

                },
                failure = { error ->
                    if (error is NetworkError &&
                        error.statusCode == HttpStatusCode.Unauthorized.value
                    ) {
                        logoutErrorAlerter.logout()
                        null
                    } else {
                        throw error
                    }
                },
            )
    }

    private suspend fun getTokensInternal(): Tokens? {
        return withContext(dispatcherProviderInterface.IO) {
            val accessToken = encryptedSettings.getString(KEY_ACCESS_TOKEN) ?: run {
                Log.e("no access token in storage")
                logoutErrorAlerter.logout()
                return@withContext null
            }

            val refreshToken = encryptedSettings.getString(KEY_REFRESH_TOKEN) ?: run {
                Log.e("no refresh token in storage")
                logoutErrorAlerter.logout()
                return@withContext null
            }

            Tokens(
                accessToken = AccessToken(accessToken),
                refreshToken = RefreshToken(refreshToken),
            )
        }
    }
}

private const val KEY_ACCESS_TOKEN = "KEY_ACCESS_TOKEN"
private const val KEY_REFRESH_TOKEN = "KEY_REFRESH_TOKEN"