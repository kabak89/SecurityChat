package com.security.chat.multiplatform.common.core.network

import com.security.chat.multiplatform.common.core.error.NetworkError
import com.security.chat.multiplatform.common.log.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

public interface HttpClientFactory {
    public fun build(needAuthorization: Boolean): HttpClient
}

internal class HttpClientFactoryImpl(
    private val json: Json,
    private val engine: HttpClientEngine,
    private val tokenManager: Lazy<TokenManagerImpl>,
) : HttpClientFactory {

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
                sanitizeHeader { it.equals(HttpHeaders.Authorization, ignoreCase = true) }
                filter { it.url.encodedPath != "/refresh" }
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
                install(sessionAuthorization(tokenManager.value))
            }
        }
    }
}

private fun sessionAuthorization(tokenManager: TokenManagerImpl) =
    createClientPlugin("SessionAuthorization") {
        val requestTokenKey = AttributeKey<SessionAccessToken>("SessionAccessToken")

        onRequest { request, _ ->
            val session = tokenManager.getSessionAccessToken() ?: return@onRequest
            request.attributes.put(requestTokenKey, session)
            request.headers[HttpHeaders.Authorization] = "Bearer ${session.token.value}"
        }

        on(Send) { request ->
            val call = proceed(request)
            if (call.response.status != HttpStatusCode.Unauthorized ||
                call.request.headers[HttpHeaders.Authorization] == null
            ) {
                return@on call
            }

            val requestToken = request.attributes.getOrNull(requestTokenKey) ?: return@on call
            val tokens = tokenManager.refreshTokensForRequest(requestToken) ?: return@on call
            request.headers[HttpHeaders.Authorization] = "Bearer ${tokens.accessToken.value}"
            proceed(request)
        }
    }
