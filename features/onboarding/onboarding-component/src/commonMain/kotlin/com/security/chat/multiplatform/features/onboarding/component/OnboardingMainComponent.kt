package com.security.chat.multiplatform.features.onboarding.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.onboarding.component.api.OnboardingMainComponent
import com.security.chat.multiplatform.features.onboarding.domain.OnboardingModel
import org.koin.core.qualifier.named

internal class OnboardingMainComponentImpl(
    private val onOnboardingFinished: () -> Unit,
    componentContext: ComponentContext,
) : OnboardingMainComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_ONBOARDING_MAIN,
    ) {

    init {
        doOnCreate {
            val onboardingModel: OnboardingModel = getKoin().get()
            onboardingModel.start(parentScope = getKoin().get(named(SCOPE_ID_ONBOARDING_MAIN)))
        }
    }

    override fun onFinish() {
        onOnboardingFinished()
    }

}

public const val SCOPE_ID_ONBOARDING_MAIN: String = "SCOPE_ID_ONBOARDING_MAIN"
