package com.security.chat.multiplatform.features.settings.ui.screens.notifications

internal sealed interface NotificationsEvent {
    data object RequestPermission : NotificationsEvent
}
