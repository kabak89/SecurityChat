package com.security.chat.multiplatform.common.core.component

import androidx.lifecycle.ViewModelStore
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.log.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.component.get
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import org.koin.dsl.module

public abstract class BaseComponentImpl(
    componentContext: ComponentContext,
    scopeId: String,
) : BaseComponent, ComponentContext by componentContext, DiScopeHolder {

    override val viewModelStore: ViewModelStore = ViewModelStore()

    private var diScope: Scope? = null

    init {
        Log.d { "component ${this::class.qualifiedName} created" }

        diScope = getKoin().createScope(
            scopeId = scopeId,
            qualifier = named(scopeId),
        )

        Log.d { "scope $scopeId created" }

        val coroutineScopeModule = module {
            single(named(scopeId)) {
                val errorHandler = CoroutineExceptionHandler { _, e ->
                    Log.e(e, "error in coroutine scope in $scopeId DI scope")
                }

                val dispatcherProvider: DispatcherProviderInterface = get()
                CoroutineScope(
                    dispatcherProvider.IO +
                            SupervisorJob() +
                            errorHandler +
                            CoroutineName(scopeId),
                )
            } bind CoroutineScope::class
        }

        loadKoinModules(coroutineScopeModule)

        lifecycle.doOnDestroy {
            Log.d { "component ${this::class.qualifiedName} destroyed" }
            viewModelStore.clear()

            val scopedCoroutineScope: CoroutineScope = get(named(scopeId))
            scopedCoroutineScope.cancel()

            diScope?.close()

            unloadKoinModules(coroutineScopeModule)

            Log.d { "scope $scopeId closed" }
        }
    }

    override fun getDiScope(): Scope {
        return diScope!!
    }

}