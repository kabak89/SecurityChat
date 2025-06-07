package com.security.chat.multiplatform.common.base

import androidx.lifecycle.ViewModelStore
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.context.loadKoinModules
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import org.koin.dsl.module

abstract class BaseComponentImpl(
    componentContext: ComponentContext,
    scopeId: String,
) : BaseComponent, ComponentContext by componentContext, DiScopeHolder {

    override val viewModelStore: ViewModelStore = ViewModelStore()

    private var diScope: Scope? = null

    init {
        println("component ${this::class.simpleName} created")

        diScope = getKoin().createScope(
            scopeId = scopeId,
            qualifier = named(scopeId),
        )

        println("scope $scopeId created")

        val coroutineScopeModule = module {
            scope(named(scopeId)) {
                scoped {
                    val errorHandler = CoroutineExceptionHandler { _, e ->
                        println("error in coroutine scope in $scopeId DI scope: $e")
                    }

                    val dispatcherProvider: DispatcherProviderInterface = get()

                    CoroutineScope(dispatcherProvider.IO + SupervisorJob() + errorHandler)
                } bind CoroutineScope::class
            }
        }

        loadKoinModules(coroutineScopeModule)

        lifecycle.doOnDestroy {
            println("component ${this::class.simpleName} destroyed")
            viewModelStore.clear()

            val scopedCoroutineScope: CoroutineScope = diScope!!.get()
            scopedCoroutineScope.cancel()

            diScope?.close()

            println("scope $scopeId closed")
        }
    }

    override fun getDiScope(): Scope {
        return diScope!!
    }

}