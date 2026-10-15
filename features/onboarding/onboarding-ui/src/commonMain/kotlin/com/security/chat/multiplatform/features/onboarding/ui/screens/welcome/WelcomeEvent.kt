package com.security.chat.multiplatform.features.onboarding.ui.screens.welcome

internal sealed interface WelcomeEvent {
    data object RequestNotificationPermission : WelcomeEvent

    data object Finished : WelcomeEvent
}
