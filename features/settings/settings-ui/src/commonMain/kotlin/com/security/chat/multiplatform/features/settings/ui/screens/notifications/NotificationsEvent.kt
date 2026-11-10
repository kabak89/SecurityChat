package com.security.chat.multiplatform.features.settings.ui.screens.notifications

import com.security.chat.multiplatform.common.permission.entity.Permission

internal sealed interface NotificationsEvent {
    data class RequestPermission(val permission: Permission) : NotificationsEvent
    data object BluetoothAllowed : NotificationsEvent
}
