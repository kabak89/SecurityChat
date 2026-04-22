package com.security.chat.multiplatform.common.core.network.di

import com.security.chat.multiplatform.common.core.component.SCOPE_ID_ROOT
import com.security.chat.multiplatform.common.core.network.BuildKonfig
import com.security.chat.multiplatform.common.core.network.HttpClientFactory
import com.security.chat.multiplatform.common.core.network.HttpClientFactoryImpl
import com.security.chat.multiplatform.common.core.network.LiveEventsManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactoryImpl
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.common.core.network.entity.SocketConfig
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

public val coreNetworkModule: Module =
    module {
        single {
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
            }
        } bind Json::class

        singleOf(::HttpClientFactoryImpl) bind HttpClientFactory::class

        singleOf(::NetworkManagerFactoryImpl) bind NetworkManagerFactory::class

        single {
            LiveEventsManager(
                json = get(),
                coroutineScope = get(named(SCOPE_ID_ROOT)),
                socketConfig = get(),
                connectivityObserver = get(),
                httpClientFactory = get(),
            )
        }

        val host = BuildKonfig.baseHost

        single {
            SocketConfig(
                host = host,
                path = "/ws",
                port = 80,
            )
        }

        single {
            NetworkConfig(
                host = "http://$host",
                port = 80,
            )
        }
    }