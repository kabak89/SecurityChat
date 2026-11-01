package com.security.chat.multiplatform.features.settings.ui.screens.notifications

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor

@Immutable
internal data class NotificationsState(
    val isPermissionAllowed: Boolean?,
    val permissionRequestInProgress: Boolean,
    val alertDialogDescriptor: AlertDialogDescriptor?,
)
