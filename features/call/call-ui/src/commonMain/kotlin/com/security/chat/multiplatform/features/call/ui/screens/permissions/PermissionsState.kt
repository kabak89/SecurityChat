package com.security.chat.multiplatform.features.call.ui.screens.permissions

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor

@Immutable
internal data class PermissionsState(
    val isPermissionRequired: Boolean,
    val hasNavigatedToCall: Boolean,
    val alertDialogDescriptor: AlertDialogDescriptor?,
)
