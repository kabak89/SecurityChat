package com.security.chat.multiplatform.common.base

import androidx.lifecycle.ViewModelStore
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy

abstract class BaseComponentImpl(
    componentContext: ComponentContext,
) : BaseComponent, ComponentContext by componentContext {

    override val viewModelStore: ViewModelStore = ViewModelStore()

    init {
        lifecycle.doOnCreate {
            println("ewqeqweqw ${this::class.simpleName} doOnCreate")
        }

        lifecycle.doOnDestroy {
            println("ewqeqweqw ${this::class.simpleName} doOnDestroy")
            viewModelStore.clear()
        }
    }

}