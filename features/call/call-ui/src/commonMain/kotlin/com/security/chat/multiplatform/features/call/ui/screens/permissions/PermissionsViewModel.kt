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
import securitychat.common.localization.generated.resources.call_permission_open_settings
import securitychat.common.localization.generated.resources.call_permission_restricted_message
import securitychat.common.localization.generated.resources.call_permission_restricted_title
import securitychat.common.localization.generated.resources.common_cancel

internal class PermissionsViewModel(
    private val component: PermissionsComponent,
    private val permissionsManager: PermissionsManager,
) : BaseViewModel<PermissionsState, PermissionsEvent>() {

    override fun createInitialState(): PermissionsState = PermissionsState(
        isPermissionRequired = false,
        hasNavigatedToCall = false,
        alertDialogDescriptor = null,
    )

    override fun onViewActive() {
        super.onViewActive()

        val allowance = permissionsManager.isPermissionAllowed(Permission.RecordAudio)
        updatePermissionState(allowance)
    }

    fun onAllowClicked() {
        val allowance = permissionsManager.isPermissionAllowed(Permission.RecordAudio)
        updatePermissionState(allowance)

        if (allowance is AllowanceResult.Restricted) {
            sendEvent(PermissionsEvent.RequestRecordAudioPermission)
        }
    }

    fun onRecordAudioPermissionResult(allowance: AllowanceResult) {
        updatePermissionState(allowance)

        if (allowance is AllowanceResult.Restricted) {
            showPermissionRestrictedDialog()
        }
    }

    fun showPermissionRestrictedDialog() {
        val descriptor = AlertDialogDescriptor(
            content = AlertDialogContent(
                title = resPrintableText(StringRes.call_permission_restricted_title),
                message = resPrintableText(StringRes.call_permission_restricted_message),
                positiveButtonText = resPrintableText(StringRes.call_permission_open_settings),
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

    private fun updatePermissionState(allowance: AllowanceResult) {
        val isAllowed = allowance is AllowanceResult.Allowed
        updateState {
            it.copy(
                isPermissionRequired = !isAllowed,
                alertDialogDescriptor = if (isAllowed) null else it.alertDialogDescriptor,
            )
        }
        if (isAllowed && !currentViewState.hasNavigatedToCall) {
            updateState { it.copy(hasNavigatedToCall = true) }
            component.onPermissionsGranted()
        }
    }

    private fun close() {
        updateState { it.copy(alertDialogDescriptor = null) }
        component.onBackClicked()
    }
}
