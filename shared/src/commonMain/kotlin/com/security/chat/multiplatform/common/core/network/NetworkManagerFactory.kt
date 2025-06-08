package com.security.chat.multiplatform.common.core.network

interface NetworkManagerFactory {

    fun build(baseUrl: String): NetworkManager

}

internal class NetworkManagerFactoryImpl(
    private val httpClientFactory: HttpClientFactory,
) : NetworkManagerFactory {

    override fun build(baseUrl: String): NetworkManager {
        return NetworkManager(
            httpClient = httpClientFactory.build(),
            baseUrl = baseUrl,
        )
    }

}