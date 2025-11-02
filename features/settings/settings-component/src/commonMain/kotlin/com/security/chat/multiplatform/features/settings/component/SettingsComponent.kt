package com.security.chat.multiplatform.features.settings.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.security.chat.multiplatform.common.core.component.BaseComponent
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.common.core.component.DiScopeHolder
import kotlinx.serialization.Serializable

public interface SettingsComponent : BaseComponent, DiScopeHolder, BackHandlerOwner {

    public val childStack: Value<ChildStack<*, Child>>

    public fun onBackClicked()

    public sealed interface Child {

        public class SettingsMain(public val component: SettingsMainComponent) : Child

    }

}

public class SettingsComponentImpl(
    private val onExit: () -> Unit,
    componentContext: ComponentContext,
) : SettingsComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_SETTINGS,
    ) {

    private val navigation = StackNavigation<Params>()

    override val childStack: Value<ChildStack<*, SettingsComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Params.serializer(),
            initialConfiguration = Params.SettingsMain,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    override fun onBackClicked() {
        navigation.pop()
    }

    private fun createChild(
        params: Params,
        componentContext: ComponentContext,
    ): SettingsComponent.Child {
        return when (params) {
            Params.SettingsMain -> {
                SettingsComponent.Child.SettingsMain(
                    component = SettingsMainComponentImpl(
                        componentContext = componentContext,
                        onExit = onExit,
                    ),
                )
            }
        }
    }

    @Serializable
    private sealed class Params {

        @Serializable
        data object SettingsMain : Params()

    }

}

public const val SCOPE_ID_SETTINGS: String = "SCOPE_ID_SETTINGS"