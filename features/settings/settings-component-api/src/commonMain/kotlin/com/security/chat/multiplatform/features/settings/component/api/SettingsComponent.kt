package com.security.chat.multiplatform.features.settings.component.api

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.DiScopeHolder
import com.security.chat.multiplatform.features.settings.ui.component.SettingsMainComponent

public interface SettingsComponent : BaseComponent, DiScopeHolder, BackHandlerOwner {

    public val childStack: Value<ChildStack<*, Child>>

    public fun onBackClicked()

    public sealed interface Child {

        public class SettingsMain(public val component: SettingsMainComponent) : Child
        public class Theme(public val component: ThemeComponent) : Child

    }
}