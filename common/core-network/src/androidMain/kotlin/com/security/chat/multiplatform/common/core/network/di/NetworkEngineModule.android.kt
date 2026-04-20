package com.security.chat.multiplatform.common.core.network.di

import com.security.chat.multiplatform.common.core.component.SCOPE_ID_ROOT
import com.security.chat.multiplatform.common.core.network.ConnectivityObserver
import com.security.chat.multiplatform.common.core.network.ConnectivityObserverAndroid
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

public actual val networkEngineModule: Module =
    module {
        single { OkHttp.create() } bind HttpClientEngine::class

        single {
            ConnectivityObserverAndroid(
                context = get(),
                coroutineScope = get(named(SCOPE_ID_ROOT)),
            )
        } bind ConnectivityObserver::class
    }