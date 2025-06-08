package com.security.chat.multiplatform.features.authorize.data.repoimpl

import com.asyncant.crypto.sha256
import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.features.authorize.data.entity.AuthRequest
import com.security.chat.multiplatform.features.authorize.data.entity.AuthResponse
import com.security.chat.multiplatform.features.authorize.domain.entity.AuthResult
import com.security.chat.multiplatform.features.authorize.domain.repo.SignInRepo
import com.security.chat.multiplatform.features.user.data_storage.UserStorage
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode

class SignInRepoImpl(
    private val networkManagerFactory: NetworkManagerFactory,
    private val userStorage: UserStorage,
) : SignInRepo {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(baseUrl = "http://13.60.146.92:80")
    }

    override suspend fun authorize(username: String, password: String): AuthResult {
        return try {
            val response: AuthResponse = networkManager.runPost(
                relativePath = "/auth",
                request = AuthRequest(
                    login = username,
                    passwordHash = sha256Hash(password),
                    publicKey = "",
                ),
            )

            userStorage.saveUserId(userId = response.userId)

            AuthResult.Success
        } catch (e: Exception) {
            when (e) {
                is ClientRequestException -> {
                    when (e.response.status) {
                        HttpStatusCode.NotFound -> AuthResult.UserNotExists
                        HttpStatusCode.Forbidden -> AuthResult.WrongPassword
                        else -> throw e
                    }
                }

                else -> throw e
            }
        }
    }

    private fun sha256Hash(input: String): String {
        val bytes = input.encodeToByteArray()
        val hashBytes = sha256(bytes)
        return hashBytes.decodeToString()
    }

}