package com.security.chat.multiplatform.features.authorize.data.repoimpl

import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.features.authorize.data.entity.AuthRequest
import com.security.chat.multiplatform.features.authorize.data.entity.AuthResponse
import com.security.chat.multiplatform.features.authorize.domain.repo.SignInRepo
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import org.kotlincrypto.hash.sha2.SHA256

internal class SignInRepoImpl(
    private val networkManagerFactory: NetworkManagerFactory,
    private val userStorage: UserStorage,
    private val networkConfig: NetworkConfig,
) : SignInRepo {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(baseUrl = "${networkConfig.host}:${networkConfig.port}")
    }

    override suspend fun signIn(username: String, password: String) {
        val cryptoKeys = getKeysPair()
        userStorage.saveKeys(cryptoKeys)

        val response: AuthResponse = networkManager.runPost(
            relativePath = "/sign-in",
            request = AuthRequest(
                login = username,
                passwordHash = sha256Hash(password),
                publicKey = cryptoKeys.publicKey,
            ),
        )

        userStorage.saveUserId(userId = response.userId)
    }

    override suspend fun isOnboardingPassed(): Boolean {
        return userStorage.getIsOnboardingPassed()
    }

    private fun sha256Hash(input: String): String {
        return SHA256().digest(input.encodeToByteArray()).decodeToString()
    }
}