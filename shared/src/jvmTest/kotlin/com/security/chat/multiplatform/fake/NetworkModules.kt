package com.security.chat.multiplatform.fake

import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

internal fun mockNetworkModule(handler: MockRequestHandler): Module =
    module {
        single { NetworkConfig(host = "http://auth.test", port = 80) }
        single<HttpClientEngine> {
            MockEngine(handler)
        } onClose { engine -> engine?.close() }
    }

internal fun successfulSignInModule(): Module =
    mockNetworkModule { request ->
        check(request.method == HttpMethod.Post && request.url.encodedPath == "/sign-in") {
            "Unexpected HTTP request: ${request.method} ${request.url}"
        }
        respond(
            content = """
                {
                    "userId": "ui-test-user",
                    "accessToken": "ui-test-access",
                    "refreshToken": "ui-test-refresh"
                }
            """.trimIndent(),
            status = HttpStatusCode.OK,
            headers = headersOf(
                HttpHeaders.ContentType,
                ContentType.Application.Json.toString(),
            ),
        )
    }
