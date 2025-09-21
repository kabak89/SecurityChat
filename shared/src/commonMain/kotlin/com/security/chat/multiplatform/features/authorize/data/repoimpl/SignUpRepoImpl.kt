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
import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.ECDSA
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import org.kotlincrypto.hash.sha2.SHA256

internal class SignUpRepoImpl(
    private val networkManagerFactory: NetworkManagerFactory,
    private val userStorage: UserStorage,
) : SignUpRepo {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(baseUrl = "http://192.168.1.3:80")
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

    private suspend fun getKeysPair(): CryptoKeys {
        // getting default provider
        val provider = CryptographyProvider.Default
        // getting ECDSA algorithm
        val ecdsa = provider.get(ECDSA)

        // creating key generator with the specified curve
        val keyPairGenerator = ecdsa.keyPairGenerator(EC.Curve.P521)

        // generating ECDSA key pair
        //  types here and below are not required, and just needed to hint reader
        val keyPair: ECDSA.KeyPair = keyPairGenerator.generateKey()

        // note, the curve should be the same
//        val decodedPublicKey: ECDSA.PublicKey = ecdsa.publicKeyDecoder(EC.Curve.P521)
//            .decodeFromByteArray(EC.PublicKey.Format.DER, encodedPublicKey)
//
//        val decodedKeyVerificationResult: Boolean =
//            decodedPublicKey.signatureVerifier(digest = SHA512, format = ECDSA.SignatureFormat.DER)
//                .tryVerifySignature("text1".encodeToByteArray(), signature)
//
//        // will print true
//        println(decodedKeyVerificationResult)

        return CryptoKeys(
            publicKey = keyPair.publicKey.encodeToByteArray(EC.PublicKey.Format.DER)
                .decodeToString(),
            privateKey = keyPair.privateKey.encodeToByteArray(EC.PrivateKey.Format.DER)
                .decodeToString(),
        )
    }

}