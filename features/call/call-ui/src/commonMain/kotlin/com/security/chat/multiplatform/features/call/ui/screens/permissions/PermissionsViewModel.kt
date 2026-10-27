package com.security.chat.multiplatform.features.call.ui.screens.permissions

import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import com.security.chat.multiplatform.common.permission.PermissionsManager
import com.security.chat.multiplatform.common.permission.entity.AllowanceResult
import com.security.chat.multiplatform.common.permission.entity.Permission
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogContent
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor
import com.security.chat.multiplatform.features.call.component.api.PermissionsComponent
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import securitychat.common.localization.generated.resources.common_cancel
import securitychat.common.localization.generated.resources.permissions_local_network_restricted_message
import securitychat.common.localization.generated.resources.permissions_local_network_restricted_title
import securitychat.common.localization.generated.resources.permissions_microphone_restricted_message
import securitychat.common.localization.generated.resources.permissions_microphone_restricted_title
import securitychat.common.localization.generated.resources.permissions_open_settings

internal class PermissionsViewModel(
    private val component: PermissionsComponent,
    private val permissionsManager: PermissionsManager,
) : BaseViewModel<PermissionsState, PermissionsEvent>() {

    private val callPermissions = listOf(Permission.RecordAudio, Permission.LocalNetwork)
    private var requestedPermission: Permission? = null

    override fun createInitialState(): PermissionsState =
        PermissionsState(
            requiredPermissions = persistentListOf(),
            hasNavigatedToCall = false,
            alertDialogDescriptor = null,
        )

    override fun onViewActive() {
        super.onViewActive()

        updatePermissionState()
    }

    fun onAllowClicked() {
        if (requestedPermission != null || currentViewState.hasNavigatedToCall) return
        updatePermissionState()?.let(::requestPermission)
    }

    fun onPermissionResult(permission: Permission, allowance: AllowanceResult) {
        if (requestedPermission != permission) return
        requestedPermission = null
        val remainingPermission = updatePermissionState() ?: return
        when (allowance) {
            AllowanceResult.Allowed -> requestPermission(remainingPermission)
            AllowanceResult.Restricted -> showPermissionRestrictedDialog()
        }
    }

    fun showPermissionRestrictedDialog() {
        val permission = currentViewState.requiredPermissions.firstOrNull() ?: return
        val isLocalNetwork = permission == Permission.LocalNetwork
        val descriptor = AlertDialogDescriptor(
            content = AlertDialogContent(
                title = resPrintableText(
                    if (isLocalNetwork) StringRes.permissions_local_network_restricted_title
                    else StringRes.permissions_microphone_restricted_title,
                ),
                message = resPrintableText(
                    if (isLocalNetwork) StringRes.permissions_local_network_restricted_message
                    else StringRes.permissions_microphone_restricted_message,
                ),
                positiveButtonText = resPrintableText(StringRes.permissions_open_settings),
                negativeButtonText = resPrintableText(StringRes.common_cancel),
            ),
            dismissAction = ::close,
            positiveAction = {
                updateState { it.copy(alertDialogDescriptor = null) }
                permissionsManager.openAppSettings()
            },
            negativeAction = ::close,
        )
        updateState { it.copy(alertDialogDescriptor = descriptor) }
    }

    private fun requestPermission(permission: Permission) {
        requestedPermission = permission
        sendEvent(PermissionsEvent.RequestPermission(permission))
    }

    private fun updatePermissionState(): Permission? {
        val requiredPermissions = callPermissions.filter {
            permissionsManager.isPermissionAllowed(it) is AllowanceResult.Restricted
        }.toImmutableList()
        val isAllowed = requiredPermissions.isEmpty()
        updateState {
            it.copy(
                requiredPermissions = requiredPermissions,
                alertDialogDescriptor = if (isAllowed) null else it.alertDialogDescriptor,
            )
        }
        if (isAllowed && !currentViewState.hasNavigatedToCall) {
            updateState { it.copy(hasNavigatedToCall = true) }
            component.onPermissionsGranted()
        }
        return requiredPermissions.firstOrNull()
    }

    private fun close() {
        updateState { it.copy(alertDialogDescriptor = null) }
        component.onBackClicked()
    }
}
