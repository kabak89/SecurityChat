package com.security.chat.multiplatform.features.profile.component.api

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.DiScopeHolder

public interface ProfileComponent : BaseComponent, DiScopeHolder, BackHandlerOwner {

    public val childStack: Value<ChildStack<*, Child>>

    public fun onBackClicked()

    public sealed interface Child {

        public class ProfileMain(public val component: ProfileMainComponent) : Child
    }
}
