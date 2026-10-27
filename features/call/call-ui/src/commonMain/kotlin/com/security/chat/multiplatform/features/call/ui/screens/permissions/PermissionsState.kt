package com.security.chat.multiplatform.features.call.ui.screens.permissions

import androidx.compose.runtime.Immutable
import com.security.chat.multiplatform.common.permission.entity.Permission
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class PermissionsState(
    val requiredPermissions: ImmutableList<Permission>,
    val hasNavigatedToCall: Boolean,
    val alertDialogDescriptor: AlertDialogDescriptor?,
) {
    val isPermissionRequired: Boolean get() = requiredPermissions.isNotEmpty()
}
