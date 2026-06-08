package com.security.chat.multiplatform.features.onboarding.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.onboarding.component.api.OnboardingComponent
import com.security.chat.multiplatform.features.onboarding.component.api.OnboardingComponent.Child.OnboardingMain
import com.security.chat.multiplatform.features.onboarding.data.di.onboardingDataModule
import com.security.chat.multiplatform.features.onboarding.domain.di.onboardingDomainModule
import com.security.chat.multiplatform.features.onboarding.ui.di.onboardingUiModule
import kotlinx.serialization.Serializable

public class OnboardingComponentImpl(
    componentContext: ComponentContext,
    private val onFinished: () -> Unit,
) : OnboardingComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_ONBOARDING,
    ) {

    private val navigation = StackNavigation<Params>()

    init {
        val featureModules = listOf(
            onboardingUiModule,
            onboardingDomainModule,
            onboardingDataModule,
        )
        getKoin().loadModules(featureModules)
        doOnDestroy {
            getKoin().unloadModules(featureModules)
        }
    }

    override val childStack: Value<ChildStack<*, OnboardingComponent.Child>> =
        childStack(
            source = navigation,
            serializer = Params.serializer(),
            initialConfiguration = Params.OnboardingMain,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    override fun onBackClicked() {
        navigation.pop()
    }

    private fun createChild(
        params: Params,
        componentContext: ComponentContext,
    ): OnboardingComponent.Child {
        return when (params) {
            Params.OnboardingMain -> {
                OnboardingMain(
                    component = OnboardingMainComponentImpl(
                        componentContext = componentContext,
                        onOnboardingFinished = onFinished,
                    ),
                )
            }
        }
    }

    @Serializable
    private sealed class Params {
        @Serializable
        data object OnboardingMain : Params()
    }
}

public const val SCOPE_ID_ONBOARDING: String = "SCOPE_ID_ONBOARDING"
