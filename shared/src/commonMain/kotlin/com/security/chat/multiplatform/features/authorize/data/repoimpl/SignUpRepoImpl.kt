package com.security.chat.multiplatform.features.authorize.data.repoimpl

import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.features.authorize.data.entity.AuthRequest
import com.security.chat.multiplatform.features.authorize.data.entity.AuthResponse
import com.security.chat.multiplatform.features.authorize.domain.entity.SignUpResult
import com.security.chat.multiplatform.features.authorize.domain.repo.SignUpRepo
import com.security.chat.multiplatform.features.user.data_storage.UserStorage
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import org.kotlincrypto.hash.sha2.SHA256

internal class SignUpRepoImpl(
    private val networkManagerFactory: NetworkManagerFactory,
    private val userStorage: UserStorage,
) : SignUpRepo {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(baseUrl = "http://13.60.146.92:80")
    }

    override suspend fun signUp(username: String, password: String): SignUpResult {
        return try {
            val response: AuthResponse = networkManager.runPost(
                relativePath = "/sign-up",
                request = AuthRequest(
                    login = username,
                    passwordHash = sha256Hash(password),
                    //TODO
                    publicKey = "",
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

}