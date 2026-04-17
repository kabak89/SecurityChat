package com.security.chat.multiplatform.features.profile.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.profile.component.api.ProfileComponent
import com.security.chat.multiplatform.features.profile.data.di.profileDataModule
import com.security.chat.multiplatform.features.profile.data.storage.di.profileDataStorageModule
import com.security.chat.multiplatform.features.profile.domain.di.profileDomainModule
import com.security.chat.multiplatform.features.profile.ui.di.profileUiModule
import kotlinx.serialization.Serializable

public class ProfileComponentImpl(
    private val onExit: () -> Unit,
    componentContext: ComponentContext,
) : ProfileComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_PROFILE,
    ) {

    private val navigation = StackNavigation<Params>()

    init {
        val featureModules = listOf(
            profileUiModule,
            profileDomainModule,
            profileDataModule,
            profileDataStorageModule,
        )
        getKoin().loadModules(featureModules)
        doOnDestroy {
            getKoin().unloadModules(featureModules)
        }
    }

    override val childStack: Value<ChildStack<*, ProfileComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Params.serializer(),
            initialConfiguration = Params.ProfileMain,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    override fun onBackClicked() {
        navigation.pop()
    }

    private fun createChild(
        params: Params,
        componentContext: ComponentContext,
    ): ProfileComponent.Child {
        return when (params) {
            Params.ProfileMain -> {
                ProfileComponent.Child.ProfileMain(
                    component = ProfileMainComponentImpl(
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
        data object ProfileMain : Params()
    }
}

public const val SCOPE_ID_PROFILE: String = "SCOPE_ID_PROFILE"
