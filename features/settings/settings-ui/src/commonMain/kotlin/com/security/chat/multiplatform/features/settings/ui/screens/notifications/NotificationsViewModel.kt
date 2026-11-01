package com.security.chat.multiplatform.features.settings.ui.screens.notifications

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import com.security.chat.multiplatform.common.permission.PermissionsManager
import com.security.chat.multiplatform.common.permission.entity.AllowanceResult
import com.security.chat.multiplatform.common.permission.entity.Permission
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogContent
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import securitychat.common.localization.generated.resources.common_cancel
import securitychat.common.localization.generated.resources.notifications_open_settings
import securitychat.common.localization.generated.resources.notifications_restricted_message
import securitychat.common.localization.generated.resources.notifications_restricted_title

internal class NotificationsViewModel(
    private val permissionsManager: PermissionsManager,
) : BaseViewModel<NotificationsState, NotificationsEvent>() {

    private var permissionCheckJob: Job? = null

    override fun createInitialState(): NotificationsState =
        NotificationsState(
            isPermissionAllowed = null,
            permissionRequestInProgress = false,
            alertDialogDescriptor = null,
        )

    override fun onViewActive() {
        super.onViewActive()

        permissionCheckJob?.cancel()
        permissionCheckJob = viewModelScope.launch {
            val isAllowed = permissionsManager.checkPermission(Permission.Notifications) ==
                    AllowanceResult.Allowed
            updateState {
                it.copy(
                    isPermissionAllowed = isAllowed,
                    alertDialogDescriptor = if (isAllowed) null else it.alertDialogDescriptor,
                )
            }
        }
    }

    fun onAllowClicked() {
        if (currentViewState.isPermissionAllowed != false ||
            currentViewState.permissionRequestInProgress
        ) return

        updateState { it.copy(permissionRequestInProgress = true) }
        sendEvent(NotificationsEvent.RequestPermission)
    }

    fun onPermissionResult(allowance: AllowanceResult) {
        if (!currentViewState.permissionRequestInProgress) return

        permissionCheckJob?.cancel()
        val isAllowed = allowance == AllowanceResult.Allowed
        updateState {
            it.copy(
                isPermissionAllowed = isAllowed,
                permissionRequestInProgress = false,
                alertDialogDescriptor = null,
            )
        }
        if (!isAllowed) {
            showPermissionRestrictedDialog()
        }
    }

    private fun showPermissionRestrictedDialog() {
        val descriptor = AlertDialogDescriptor(
            content = AlertDialogContent(
                title = resPrintableText(StringRes.notifications_restricted_title),
                message = resPrintableText(StringRes.notifications_restricted_message),
                positiveButtonText = resPrintableText(StringRes.notifications_open_settings),
                negativeButtonText = resPrintableText(StringRes.common_cancel),
            ),
            dismissAction = ::dismissDialog,
            positiveAction = {
                dismissDialog()
                permissionsManager.openAppSettings()
            },
            negativeAction = ::dismissDialog,
        )
        updateState { it.copy(alertDialogDescriptor = descriptor) }
    }

    private fun dismissDialog() {
        updateState { it.copy(alertDialogDescriptor = null) }
    }
}
