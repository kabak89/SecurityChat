package com.security.chat.multiplatform.features.authorize.data.repoimpl

import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.TokenManager
import com.security.chat.multiplatform.common.core.network.entity.AccessToken
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.common.core.network.entity.RefreshToken
import com.security.chat.multiplatform.common.core.network.entity.Tokens
import com.security.chat.multiplatform.common.device.info.DeviceInfoManager
import com.security.chat.multiplatform.features.authorize.data.entity.SignInRequest
import com.security.chat.multiplatform.features.authorize.data.entity.SignInResponse
import com.security.chat.multiplatform.features.authorize.domain.repo.SignInRepo
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import com.security.chat.multiplatform.features.user.data.storage.entity.CryptoKeys
import kotlin.uuid.Uuid

internal class SignInRepoImpl(
    private val networkManagerFactory: NetworkManagerFactory,
    private val userStorage: UserStorage,
    private val networkConfig: NetworkConfig,
    private val tokenManager: TokenManager,
    private val deviceInfoManager: DeviceInfoManager,
) : SignInRepo {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(
            baseUrl = "${networkConfig.host}:${networkConfig.port}",
            needAuthorization = false,
        )
    }

    override suspend fun signIn(privateKey: String) {
        val deviceId = Uuid.random().toString()
        userStorage.saveDeviceId(id = deviceId)

        val response: SignInResponse = networkManager.runPost(
            relativePath = "/sign-in",
            request = SignInRequest(
                privateKeyHash = sha256Hash(privateKey),
                deviceId = deviceId,
                deviceName = deviceInfoManager.getDeviceName(),
            ),
        )

        val cryptoKeys = CryptoKeys(
            publicKey = derivePublicKey(privateKey),
            privateKey = privateKey,
        )
        userStorage.saveKeys(cryptoKeys)
        userStorage.saveUserId(userId = response.userId)
        tokenManager.saveTokens(
            tokens = Tokens(
                accessToken = AccessToken(response.accessToken),
                refreshToken = RefreshToken(response.refreshToken),
            ),
        )
    }

    override suspend fun isOnboardingPassed(): Boolean {
        return userStorage.getIsOnboardingPassed()
    }
}