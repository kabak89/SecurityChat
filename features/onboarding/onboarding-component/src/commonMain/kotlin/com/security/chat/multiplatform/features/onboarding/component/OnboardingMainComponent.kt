package com.security.chat.multiplatform.features.onboarding.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.onboarding.component.api.OnboardingMainComponent

internal class OnboardingMainComponentImpl(
    private val onOnboardingFinished: () -> Unit,
    componentContext: ComponentContext,
) : OnboardingMainComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_ONBOARDING_MAIN,
    ) {

    override fun onFinish() {
        onOnboardingFinished()
    }

}

public const val SCOPE_ID_ONBOARDING_MAIN: String = "SCOPE_ID_ONBOARDING_MAIN"
