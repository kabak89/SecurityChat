package com.security.chat.multiplatform.common.core.network

interface NetworkManagerFactory {

    fun build(baseUrl: String): InternalNetworkManager

}

internal class NetworkManagerFactoryImpl(
    private val httpClientFactory: HttpClientFactory,
) : NetworkManagerFactory {

    override fun build(baseUrl: String): InternalNetworkManager {
        return InternalNetworkManager(
            httpClient = httpClientFactory.build(),
            baseUrl = baseUrl,
        )
    }

}