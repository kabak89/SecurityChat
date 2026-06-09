package com.security.chat.multiplatform.features.onboarding.ui.screens.notificationpermission

internal sealed interface NotificationPermissionEvent {
    data object Finished : NotificationPermissionEvent
}
