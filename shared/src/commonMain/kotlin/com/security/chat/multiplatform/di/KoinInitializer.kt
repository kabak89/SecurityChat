package com.security.chat.multiplatform.di

import com.security.chat.multiplatform.common.core.component.SCOPE_ID_APP
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.crash.report.initCrashReporting
import com.security.chat.multiplatform.common.log.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.loadKoinModules
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module

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
