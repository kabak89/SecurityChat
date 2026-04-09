package com.security.chat.multiplatform.features.authorize.data.repoimpl

import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.features.authorize.data.entity.AuthRequest
import com.security.chat.multiplatform.features.authorize.data.entity.AuthResponse
import com.security.chat.multiplatform.features.authorize.domain.entity.SignUpResult
import com.security.chat.multiplatform.features.authorize.domain.repo.SignUpRepo
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import com.security.chat.multiplatform.features.user.data.storage.entity.CryptoKeys
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.operations.KeyGenerator
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import org.kotlincrypto.hash.sha2.SHA256
import kotlin.io.encoding.Base64

internal class SignUpRepoImpl(
    private val networkManagerFactory: NetworkManagerFactory,
    private val userStorage: UserStorage,
) : SignUpRepo {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(baseUrl = "http://192.168.1.5:80")
    }

    override suspend fun signUp(username: String, password: String): SignUpResult {
        return try {
            val cryptoKeys = getKeysPair()
            userStorage.saveKeys(cryptoKeys)

            val response: AuthResponse = networkManager.runPost(
                relativePath = "/sign-up",
                request = AuthRequest(
                    login = username,
                    passwordHash = sha256Hash(password),
                    publicKey = cryptoKeys.publicKey,
                ),
            )

            userStorage.saveUserId(userId = response.userId)

            SignUpResult.Success
        } catch (e: Exception) {
            when (e) {
                is ClientRequestException -> {
                    when (e.response.status) {
                        HttpStatusCode.Forbidden -> SignUpResult.LoginAlreadyExists
                        else -> throw e
                    }
                }

                else -> throw e
            }
        }
    }

    private fun sha256Hash(input: String): String {
        return SHA256().digest(input.encodeToByteArray()).decodeToString()
    }

    @OptIn(DelicateCryptographyApi::class)
    private suspend fun getKeysPair(): CryptoKeys {
        val provider = CryptographyProvider.Default
        val rsa = provider.get(RSA.RAW)
        val keyPairGenerator: KeyGenerator<RSA.RAW.KeyPair> = rsa.keyPairGenerator()
        val keyPair = keyPairGenerator.generateKey()
        val publicKey = keyPair.publicKey.encodeToByteArray(RSA.PublicKey.Format.DER)
        val publicString = Base64.encode(publicKey)
        val privateKeyString = keyPair.privateKey.encodeToByteArray(RSA.PrivateKey.Format.DER)
        val privateKey = Base64.encode(privateKeyString)

        return CryptoKeys(
            publicKey = publicString,
            privateKey = privateKey,
        )
    }

}