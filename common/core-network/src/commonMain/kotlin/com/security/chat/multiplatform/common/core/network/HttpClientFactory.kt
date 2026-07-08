package com.security.chat.multiplatform.common.core.network

import com.security.chat.multiplatform.common.core.error.NetworkError
import com.security.chat.multiplatform.common.log.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

public interface HttpClientFactory {
    public fun build(needAuthorization: Boolean): HttpClient
}

internal class HttpClientFactoryImpl(
    private val json: Json,
    private val engine: HttpClientEngine,
) : HttpClientFactory, KoinComponent {

    override fun build(needAuthorization: Boolean): HttpClient {
        val networkLogger: Logger = object : Logger {
            override fun log(message: String) {
                Log.d { message }
            }
        }

        return HttpClient(engine = engine) {
            install(WebSockets)
            install(ContentNegotiation) {
                json(json)
            }
            install(Logging) {
                logger = networkLogger
                level = LogLevel.ALL
            }
            expectSuccess = true
            HttpResponseValidator {
                handleResponseExceptionWithRequest { exception, _ ->
                    when (exception) {
                        is ClientRequestException -> {
                            val exceptionResponse = exception.response
                            val status = exceptionResponse.status
                            throw NetworkError(statusCode = status.value)
                        }

                        is CancellationException -> throw exception

                        else -> throw resolvePlatformError(exception) ?: resolveError(exception)
                    }
                }
            }
            if (needAuthorization) {
                val tokenManager: TokenManager = get()

                install(Auth) {
                    bearer {
                        loadTokens {
                            val tokens = tokenManager.getTokens() ?: return@loadTokens null
                            BearerTokens(
                                accessToken = tokens.accessToken.value,
                                refreshToken = tokens.refreshToken.value,
                            )
                        }
                        refreshTokens {
                            val tokens = tokenManager.refreshTokens() ?: return@refreshTokens null

                            BearerTokens(
                                accessToken = tokens.accessToken.value,
                                refreshToken = tokens.refreshToken.value,
                            )
                        }
                    }
                }
            }
        }
    }
}