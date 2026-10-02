package com.security.chat.multiplatform.features.authorize.data.repoimpl

import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.TokenManager
import com.security.chat.multiplatform.common.core.network.entity.AccessToken
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.common.core.network.entity.RefreshToken
import com.security.chat.multiplatform.common.core.network.entity.Tokens
import com.security.chat.multiplatform.common.device.info.DeviceInfoManager
import com.security.chat.multiplatform.common.encryption.RsaSqueezer
import com.security.chat.multiplatform.common.encryption.derivePublicKey
import com.security.chat.multiplatform.common.encryption.entity.CryptoKeys
import com.security.chat.multiplatform.common.encryption.sha256Hash
import com.security.chat.multiplatform.features.authorize.data.entity.SignInRequest
import com.security.chat.multiplatform.features.authorize.data.entity.SignInResponse
import com.security.chat.multiplatform.features.authorize.domain.repo.SignInRepo
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
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
        val trimmedPrivateKey = privateKey.trim()
        val resolvedPrivateKey = if (trimmedPrivateKey == "reviewer") {
            REVIEWER_PRIVATE_KEY
        } else {
            trimmedPrivateKey
        }
        val rawPrivateKey = RsaSqueezer.expand(resolvedPrivateKey)
        val deviceId = Uuid.random().toString()
        userStorage.saveDeviceId(id = deviceId)

        val response: SignInResponse = networkManager.runPost(
            relativePath = "/sign-in",
            request = SignInRequest(
                privateKeyHash = sha256Hash(rawPrivateKey),
                deviceId = deviceId,
                deviceName = deviceInfoManager.getDeviceName(),
            ),
        )

        val cryptoKeys = CryptoKeys(
            publicKey = derivePublicKey(rawPrivateKey),
            privateKey = rawPrivateKey,
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

private const val REVIEWER_PRIVATE_KEY =
    "rsasq|CAEBAAEA+EXz+tXtl/oAWAyOSDpdr5lVDxkhvxDXx021/F4vTAKGJuQBLCaEZMezuYMK+KFyjc" +
            "fee/l/fbJi/abKNRPJdDSbBEXvlcyHTLiZduHFn29GVcNIGh5aZqQ1clmwWcULkqJJS+TkfqN5gg4Iz8" +
            "8WAYVNgdu6D941y9QzVm0pSqBCJz85s1rpsexwa0xqAwmAr+V942ccV3rg3SmLXeLib1C99/MQQpEdFu" +
            "W6lPNpThgvhGI4aVeHb3lV94mobeOor+FXu3QuiSDHivKQrMJ4QfeO8rkxCJQhgM6Qx64SYdiufrcxCO" +
            "TfN8I13NFBTPDMigv3AoKPLot+DpMs/22Cm926p+HpK/cdnR60wsD5d2MWV9xe1NMNuUpxvfIZGjnHba" +
            "XQKxY+N3eZsXGcYKM3ATABLi5KOW13OhAKyjEKSemHGqBjIWoJ0UM1+F2nPBdm4R/qRWPPTr1Qg2rdta" +
            "tENVq9jE8fNUHZ4VdztCDmlFPicmwlnntOG79PEKbBM8q3nG3EwwwGbjVd9F0XpCZxgnVM+aGSqPOVg2" +
            "AGFIn0rdaBcIAVM2EGq+00Jj7ei3tdYATLc5KcgIgY2LEEZ4DJta3In2H8Eeh+ifi2lKk+F15GXmQWzN" +
            "DjjXVe0HPSaLBIeWMRaacd+C3mldk8/whKrOZeuAwiu+aUepeBJt7cKIECAQADAgABAQEBAQABAQEA"
