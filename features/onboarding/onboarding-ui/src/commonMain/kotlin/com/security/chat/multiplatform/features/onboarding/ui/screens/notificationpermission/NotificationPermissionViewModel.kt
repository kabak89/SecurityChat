package com.security.chat.multiplatform.features.onboarding.ui.screens.notificationpermission

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import com.security.chat.multiplatform.common.permission.PermissionsManager
import com.security.chat.multiplatform.common.permission.entity.AllowanceResult
import com.security.chat.multiplatform.common.permission.entity.Permission
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogContent
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor
import com.security.chat.multiplatform.features.onboarding.domain.OnboardingModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.kode.remo.successResults
import securitychat.common.localization.generated.resources.common_ok
import securitychat.common.localization.generated.resources.notification_permission_restricted_permission_dialog_message
import securitychat.common.localization.generated.resources.notification_permission_restricted_permission_dialog_title

internal class NotificationPermissionViewModel(
    private val onboardingModel: OnboardingModel,
    private val permissionsManager: PermissionsManager,
) : BaseViewModel<NotificationPermissionState, NotificationPermissionEvent>() {

    override fun onPostStart() {
        super.onPostStart()

        onboardingModel.finishOnboarding.jobFlow.successResults()
            .onEach {
                sendEvent(NotificationPermissionEvent.Finished)
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            if (permissionsManager.checkPermission(Permission.Notifications) == AllowanceResult.Restricted) {
                updateState { it.copy(permission = Permission.Notifications) }
            } else {
                showBluetoothOrFinish()
            }
        }
    }

    override fun createInitialState(): NotificationPermissionState {
        return NotificationPermissionState(
            permission = null,
            alertDialogDescriptor = null,
        )
    }

    fun onPermissionResult(permission: Permission, allowance: AllowanceResult) {
        if (currentViewState.permission != permission) return
        if (permission == Permission.Notifications && allowance == AllowanceResult.Restricted) {
            showNotificationRestrictionDialog()
        } else {
            advanceAfter(permission, allowance)
        }
    }

    private fun showNotificationRestrictionDialog() {
        val alertDialogDescriptor = AlertDialogDescriptor(
            content = AlertDialogContent(
                title = resPrintableText(
                    StringRes.notification_permission_restricted_permission_dialog_title,
                ),
                message = resPrintableText(
                    StringRes.notification_permission_restricted_permission_dialog_message,
                ),
                positiveButtonText = resPrintableText(StringRes.common_ok),
            ),
            dismissAction = { advanceAfter(Permission.Notifications, AllowanceResult.Restricted) },
            positiveAction = { advanceAfter(Permission.Notifications, AllowanceResult.Restricted) },
        )
        updateState { it.copy(alertDialogDescriptor = alertDialogDescriptor) }
    }

    private fun advanceAfter(permission: Permission, allowance: AllowanceResult) {
        if (currentViewState.permission != permission) return
        updateState { it.copy(permission = null, alertDialogDescriptor = null) }
        when {
            permission == Permission.Notifications -> viewModelScope.launch { showBluetoothOrFinish() }
            permission == Permission.Bluetooth && allowance == AllowanceResult.Allowed ->
                viewModelScope.launch { showBackgroundLocationOrFinish() }

            else -> onboardingModel.finishOnboarding.start()
        }
    }

    private suspend fun showBluetoothOrFinish() {
        if (permissionsManager.checkPermission(Permission.Bluetooth) == AllowanceResult.Restricted) {
            updateState { it.copy(permission = Permission.Bluetooth) }
        } else {
            showBackgroundLocationOrFinish()
        }
    }

    private suspend fun showBackgroundLocationOrFinish() {
        if (permissionsManager.bluetoothScanRequiresBackgroundLocation() &&
            permissionsManager.checkPermission(Permission.BluetoothBackgroundLocation) == AllowanceResult.Restricted
        ) {
            updateState { it.copy(permission = Permission.BluetoothBackgroundLocation) }
        } else {
            onboardingModel.finishOnboarding.start()
        }
    }
}
