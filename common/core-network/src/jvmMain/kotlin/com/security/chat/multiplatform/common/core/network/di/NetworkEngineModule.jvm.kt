package com.security.chat.multiplatform.common.core.network.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.apache5.Apache5
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

public actual val networkEngineModule: Module =
    module {
        single { HttpClient(Apache5).engine } bind HttpClientEngine::class
    }