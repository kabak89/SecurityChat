package com.security.chat.multiplatform.features.call.ui.screens.permissions

import com.security.chat.multiplatform.common.permission.entity.Permission

internal sealed interface PermissionsEvent {

    data class RequestPermission(val permission: Permission) : PermissionsEvent
}
