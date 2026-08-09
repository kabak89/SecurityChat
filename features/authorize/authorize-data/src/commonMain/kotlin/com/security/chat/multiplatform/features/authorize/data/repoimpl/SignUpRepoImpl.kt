package com.security.chat.multiplatform.features.authorize.data.repoimpl

import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.TokenManager
import com.security.chat.multiplatform.common.core.network.entity.AccessToken
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.common.core.network.entity.RefreshToken
import com.security.chat.multiplatform.common.core.network.entity.Tokens
import com.security.chat.multiplatform.common.device.info.DeviceInfoManager
import com.security.chat.multiplatform.features.authorize.data.entity.SignUpRequest
import com.security.chat.multiplatform.features.authorize.data.entity.SignUpResponse
import com.security.chat.multiplatform.features.authorize.domain.repo.SignUpRepo
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import kotlin.uuid.Uuid

internal class SignUpRepoImpl(
    private val networkManagerFactory: NetworkManagerFactory,
    private val userStorage: UserStorage,
    private val networkConfig: NetworkConfig,
    private val tokenManager: TokenManager,
    private val deviceInfoManager: DeviceInfoManager,
) : SignUpRepo {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(
            baseUrl = "${networkConfig.host}:${networkConfig.port}",
            needAuthorization = false,
        )
    }

    override suspend fun signUp(username: String) {
        val cryptoKeys = generateKeysPair()

        val deviceId = Uuid.random().toString()
        userStorage.saveDeviceId(id = deviceId)

        val response: SignUpResponse = networkManager.runPost(
            relativePath = "/sign-up",
            request = SignUpRequest(
                login = username,
                publicKey = cryptoKeys.publicKey,
                privateKeyHash = sha256Hash(cryptoKeys.privateKey),
                deviceId = deviceId,
                deviceName = deviceInfoManager.getDeviceName(),
            ),
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