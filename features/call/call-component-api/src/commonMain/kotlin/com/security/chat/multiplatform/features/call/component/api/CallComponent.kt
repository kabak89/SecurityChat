package com.security.chat.multiplatform.features.call.component.api

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface CallComponent : BaseComponent, DiScopeHolder, BackHandlerOwner {

    public val childStack: Value<ChildStack<*, Child>>

    public fun onBackClicked()

    public sealed interface Child {
        public class Main(public val component: CallMainComponent) : Child
    }
}
