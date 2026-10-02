package com.security.chat.multiplatform.features.call.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.call.component.api.CallComponent
import com.security.chat.multiplatform.features.call.data.di.callDataModule
import com.security.chat.multiplatform.features.call.domain.di.callDomainModule
import com.security.chat.multiplatform.features.call.ui.di.callUiModule
import kotlinx.serialization.Serializable

public class CallComponentImpl(
    private val chatId: String,
    private val onBack: () -> Unit,
    componentContext: ComponentContext,
) : CallComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_CALL,
    ) {

    private val navigation = StackNavigation<Params>()

    init {
        val featureModules = listOf(
            callUiModule,
            callDomainModule,
            callDataModule,
        )
        getKoin().loadModules(featureModules)
        doOnDestroy {
            getKoin().unloadModules(featureModules)
        }
    }

    override val childStack: Value<ChildStack<*, CallComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Params.serializer(),
            initialConfiguration = Params.Permissions,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    override fun onBackClicked() {
        onBack()
    }

    private fun createChild(
        params: Params,
        componentContext: ComponentContext,
    ): CallComponent.Child {
        return when (params) {
            Params.Permissions -> CallComponent.Child.Permissions(
                component = PermissionsComponentImpl(
                    onBack = ::onBackClicked,
                    onGranted = { navigation.replaceAll(Params.Main) },
                    componentContext = componentContext,
                ),
            )

            Params.Main -> CallComponent.Child.Main(
                component = CallMainComponentImpl(
                    chatId = chatId,
                    onBack = ::onBackClicked,
                    componentContext = componentContext,
                ),
            )
        }
    }

    @Serializable
    private sealed interface Params {
        @Serializable
        data object Permissions : Params

        @Serializable
        data object Main : Params
    }
}

private const val SCOPE_ID_CALL: String = "SCOPE_ID_CALL"
