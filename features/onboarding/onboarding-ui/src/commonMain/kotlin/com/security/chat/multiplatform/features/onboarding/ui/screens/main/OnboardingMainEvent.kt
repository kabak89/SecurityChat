package com.security.chat.multiplatform.features.onboarding.ui.screens.main

internal sealed interface OnboardingMainEvent {
    data object Finished : OnboardingMainEvent
}
