package com.security.chat.multiplatform.features.onboarding.ui.screens.welcome

internal sealed interface WelcomeEvent {
    data object RequestPermissions : WelcomeEvent

    data object Finished : WelcomeEvent
}
