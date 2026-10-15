package com.security.chat.multiplatform.features.onboarding.component

import com.arkivanov.decompose.ComponentContext
import com.security.chat.multiplatform.common.core.component.BaseComponentImpl
import com.security.chat.multiplatform.features.onboarding.component.api.WelcomeComponent

internal class WelcomeComponentImpl(
    private val onContinueClicked: () -> Unit,
    private val onOnboardingFinished: () -> Unit,
    componentContext: ComponentContext,
) : WelcomeComponent,
    BaseComponentImpl(
        componentContext = componentContext,
        scopeId = SCOPE_ID_ONBOARDING_WELCOME,
    ) {

    override fun onContinue() {
        onContinueClicked()
    }

    override fun onFinish() {
        onOnboardingFinished()
    }
}

private const val SCOPE_ID_ONBOARDING_WELCOME: String = "SCOPE_ID_ONBOARDING_WELCOME"
