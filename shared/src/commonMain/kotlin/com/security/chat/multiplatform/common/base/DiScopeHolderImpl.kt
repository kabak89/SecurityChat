package com.security.chat.multiplatform.common.base

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope

class DiScopeHolderImpl(
    componentContext: ComponentContext,
    scopeId: String,
) : DiScopeHolder, ComponentContext by componentContext {

    private var diScope: Scope? = null

    init {
        lifecycle.doOnCreate {
            diScope = getKoin().createScope(
                scopeId = scopeId,
                qualifier = named(scopeId),
            )

            println("ewqeqweqw scope $scopeId created")
        }

        lifecycle.doOnDestroy {
            diScope?.close()
        }
    }

    override fun getDiScope(): Scope {
        return diScope!!
    }


}