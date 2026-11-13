package com.security.chat.multiplatform.di

import com.security.chat.multiplatform.common.core.component.SCOPE_ID_APP
import com.security.chat.multiplatform.common.core.network.ConnectivityObserver
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.crash.report.initCrashReporting
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.chat.data.common.OutgoingMessageScheduler
import com.security.chat.multiplatform.features.mesh.domain.MeshModel
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.context.loadKoinModules
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

public fun initDI(appDeclaration: KoinAppDeclaration = {}) {
    initCrashReporting()

    initKoin(appDeclaration = appDeclaration)

    val coroutineScopeModule = module {
        single(named(SCOPE_ID_APP)) {
            val errorHandler = CoroutineExceptionHandler { _, e ->
                Log.e(e, "error in coroutine scope in $SCOPE_ID_APP DI scope")
            }

            val dispatcherProvider: DispatcherProviderInterface = get()
            CoroutineScope(
                dispatcherProvider.IO +
                        SupervisorJob() +
                        errorHandler +
                        CoroutineName(SCOPE_ID_APP),
            )
        } bind CoroutineScope::class
    }

    loadKoinModules(
        listOf(
            coroutineScopeModule,
        ),
    )
}

internal expect fun initKoin(appDeclaration: KoinAppDeclaration = {})

internal fun startMeshModel() {
    val koin = KoinPlatform.getKoin()
    koin.get<MeshModel>().start(parentScope = koin.get(named(SCOPE_ID_APP)))
}

internal fun startOutgoingMessages() {
    val koin = KoinPlatform.getKoin()
    val scheduler = koin.get<OutgoingMessageScheduler>()
    val userStorage = koin.get<UserStorage>()
    val connectivity = koin.get<ConnectivityObserver>()
    var wasOnline: Boolean? = null
    connectivity.isOnline.onEach { isOnline ->
        val previous = wasOnline
        wasOnline = isOnline
        if (previous == null || (!previous && isOnline)) {
            userStorage.getUserId()?.let { scheduler.schedule(it) }
        }
    }.launchIn(koin.get(named(SCOPE_ID_APP)))
}
