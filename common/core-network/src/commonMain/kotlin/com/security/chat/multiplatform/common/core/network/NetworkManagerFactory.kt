package com.security.chat.multiplatform.common.core.network

public interface NetworkManagerFactory {

    public fun build(
        baseUrl: String,
        needAuthorization: Boolean,
    ): NetworkManager

}

internal class NetworkManagerFactoryImpl(
    private val httpClientFactory: HttpClientFactory,
) : NetworkManagerFactory {

    override fun build(
        baseUrl: String,
        needAuthorization: Boolean,
    ): NetworkManager {
        return NetworkManager(
            httpClient = httpClientFactory.build(needAuthorization = needAuthorization),
            baseUrl = baseUrl,
        )
    }

}