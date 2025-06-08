package com.security.chat.multiplatform.common.core.network.di

import com.security.chat.multiplatform.common.core.network.HttpClientFactory
import com.security.chat.multiplatform.common.core.network.HttpClientFactoryImpl
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactoryImpl
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val coreNetworkModule: Module =
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
    }