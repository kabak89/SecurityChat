package com.security.chat.multiplatform.features.authorize.data.repoimpl

import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.features.authorize.data.entity.AuthRequest
import com.security.chat.multiplatform.features.authorize.data.entity.AuthResponse
import com.security.chat.multiplatform.features.authorize.domain.entity.SignInResult
import com.security.chat.multiplatform.features.authorize.domain.repo.SignInRepo
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import org.kotlincrypto.hash.sha2.SHA256

internal class SignInRepoImpl(
    private val networkManagerFactory: NetworkManagerFactory,
    private val userStorage: UserStorage,
) : SignInRepo {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(baseUrl = "http://192.168.1.5:80")
    }

    override suspend fun signIn(username: String, password: String): SignInResult {
        return try {
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

            SignInResult.Success
        } catch (e: Exception) {
            when (e) {
                is ClientRequestException -> {
                    when (e.response.status) {
                        HttpStatusCode.NotFound -> SignInResult.UserNotExists
                        HttpStatusCode.Forbidden -> SignInResult.WrongPassword
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

}