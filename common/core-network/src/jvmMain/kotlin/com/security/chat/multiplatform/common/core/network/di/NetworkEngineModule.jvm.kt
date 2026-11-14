package com.security.chat.multiplatform.common.core.network.di

import com.security.chat.multiplatform.common.core.component.SCOPE_ID_APP
import com.security.chat.multiplatform.common.core.network.ConnectivityObserver
import com.security.chat.multiplatform.common.core.network.ConnectivityObserverJvm
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
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
            ConnectivityObserverJvm(
                networkConfig = get(),
                coroutineScope = get(named(SCOPE_ID_APP)),
                ioDispatcher = get<DispatcherProviderInterface>().IO,
            )
        } bind ConnectivityObserver::class
    }
