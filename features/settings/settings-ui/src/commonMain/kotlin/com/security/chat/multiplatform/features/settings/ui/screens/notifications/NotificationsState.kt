package com.security.chat.multiplatform.features.settings.ui.screens.notifications

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.permission.entity.Permission

@Immutable
internal data class NotificationsState(
    val permissions: List<Permission>,
    val allowedPermissions: Map<Permission, Boolean>,
    val permissionRequestInProgress: Permission?,
    val restrictedPermission: Permission?,
)
