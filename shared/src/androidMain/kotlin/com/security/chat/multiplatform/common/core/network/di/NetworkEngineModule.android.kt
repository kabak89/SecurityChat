package com.security.chat.multiplatform.common.core.network.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual val networkEngineModule: Module =
    module {
        single { OkHttp.create() } bind HttpClientEngine::class
    }