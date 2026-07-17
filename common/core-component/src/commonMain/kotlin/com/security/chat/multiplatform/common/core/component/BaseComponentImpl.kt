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
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
public abstract class BaseComponentImpl(
    componentContext: ComponentContext,
    scopeId: String,
) : BaseComponent, ComponentContext by componentContext, DiScopeHolder {

    override val viewModelStore: ViewModelStore = ViewModelStore()

    /**
     * Unique per-instance scope id. Decompose creates a new child before destroying the removed
     * one, so two components of the same type may coexist for a short moment during navigation.
     * A fixed [scopeId] would make them collide on [org.koin.core.Koin.createScope]; the random
     * suffix keeps every instance isolated.
     */
    private val uniqueScopeId: String = "$scopeId-${Uuid.random()}"

    private val scopeQualifier: Qualifier = named(uniqueScopeId)

    private var diScope: Scope? = null

    /** Coroutine scope bound to this component's DI scope lifecycle. */
    protected val componentCoroutineScope: CoroutineScope
        get() = get(scopeQualifier)

    init {
        Log.d { "component ${this::class.qualifiedName} created" }

        diScope = getKoin().createScope(
            scopeId = uniqueScopeId,
            qualifier = scopeQualifier,
        )

        Log.d { "scope $uniqueScopeId created" }

        val coroutineScopeModule = module {
            single(scopeQualifier) {
                val errorHandler = CoroutineExceptionHandler { _, e ->
                    Log.e(e, "error in coroutine scope in $uniqueScopeId DI scope")
                }

                val dispatcherProvider: DispatcherProviderInterface = get()
                CoroutineScope(
                    dispatcherProvider.IO +
                            SupervisorJob() +
                            errorHandler +
                            CoroutineName(uniqueScopeId),
                )
            } bind CoroutineScope::class
        }

        loadKoinModules(coroutineScopeModule)

        lifecycle.doOnDestroy {
            Log.d { "component ${this::class.qualifiedName} destroyed" }
            viewModelStore.clear()

            val scopedCoroutineScope: CoroutineScope = get(scopeQualifier)
            scopedCoroutineScope.cancel()

            diScope?.close()

            unloadKoinModules(coroutineScopeModule)

            Log.d { "scope $uniqueScopeId closed" }
        }
    }

    override fun getDiScope(): Scope {
        return diScope!!
    }

}