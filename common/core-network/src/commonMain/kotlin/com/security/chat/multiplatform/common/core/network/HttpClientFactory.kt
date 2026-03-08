package com.security.chat.multiplatform.common.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

public interface HttpClientFactory {
    public fun build(): HttpClient
}

internal class HttpClientFactoryImpl(
    private val json: Json,
    private val engine: HttpClientEngine,
) : HttpClientFactory {

    override fun build(): HttpClient {
        val networkLogger: Logger = object : Logger {
            override fun log(message: String) {
                println(message)
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
        }
    }
}