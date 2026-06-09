package com.security.chat.multiplatform.features.onboarding.ui.screens.notificationpermission

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor

@Immutable
internal data class NotificationPermissionState(
    val alertDialogDescriptor: AlertDialogDescriptor?,
)
