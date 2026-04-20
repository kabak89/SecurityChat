package com.security.chat.multiplatform.common.core.network.di

import com.security.chat.multiplatform.common.core.network.ConnectivityObserver
import com.security.chat.multiplatform.common.core.network.ConnectivityObserverIos
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public actual val networkEngineModule: Module =
    module {
        single { Darwin.create() } bind HttpClientEngine::class

        singleOf(::ConnectivityObserverIos) bind ConnectivityObserver::class
    }