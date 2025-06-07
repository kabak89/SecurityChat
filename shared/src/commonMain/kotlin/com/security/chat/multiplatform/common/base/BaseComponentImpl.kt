package com.security.chat.multiplatform.common.base

import androidx.lifecycle.ViewModelStore
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope

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