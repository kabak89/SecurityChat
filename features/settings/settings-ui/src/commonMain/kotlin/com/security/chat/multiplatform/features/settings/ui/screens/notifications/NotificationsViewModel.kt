package com.security.chat.multiplatform.features.settings.ui.screens.notifications

import androidx.lifecycle.viewModelScope
import com.security.chat.multiplatform.common.core.ui.BaseViewModel
import com.security.chat.multiplatform.common.device.info.DeviceInfoManager
import com.security.chat.multiplatform.common.device.info.entity.Platform
import com.security.chat.multiplatform.common.permission.PermissionsManager
import com.security.chat.multiplatform.common.permission.entity.AllowanceResult
import com.security.chat.multiplatform.common.permission.entity.Permission
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class NotificationsViewModel(
    private val permissionsManager: PermissionsManager,
    deviceInfoManager: DeviceInfoManager,
) : BaseViewModel<NotificationsState, NotificationsEvent>() {

    private val permissions = buildList {
        add(Permission.Notifications)
        if (deviceInfoManager.getPlatform() != Platform.Desktop) {
            add(Permission.Bluetooth)
            if (permissionsManager.bluetoothScanRequiresBackgroundLocation()) {
                add(Permission.BluetoothBackgroundLocation)
            }
        }
    }
    private var permissionCheckJob: Job? = null

    override fun createInitialState(): NotificationsState {
        return NotificationsState(
            permissions = permissions,
            allowedPermissions = emptyMap(),
            permissionRequestInProgress = null,
            restrictedPermission = null,
        )
    }

    override fun onViewActive() {
        super.onViewActive()

        permissionCheckJob?.cancel()
        permissionCheckJob = viewModelScope.launch {
            val allowed = permissions.associateWith { permission ->
                permissionsManager.checkPermission(permission) == AllowanceResult.Allowed
            }
            val bluetoothJustAllowed = allowed.any { (permission, isAllowed) ->
                permission.affectsBluetooth() && isAllowed &&
                        currentViewState.allowedPermissions[permission] != true
            }
            updateState {
                it.copy(
                    allowedPermissions = allowed,
                    restrictedPermission = it.restrictedPermission?.takeUnless { allowed[it] == true },
                )
            }
            if (bluetoothJustAllowed) sendEvent(NotificationsEvent.BluetoothAllowed)
        }
    }

    fun onAllowClicked(permission: Permission) {
        if (currentViewState.allowedPermissions[permission] != false ||
            currentViewState.permissionRequestInProgress != null
        ) return
        if (permission == Permission.BluetoothBackgroundLocation &&
            currentViewState.allowedPermissions[Permission.Bluetooth] != true
        ) {
            return
        }

        updateState { it.copy(permissionRequestInProgress = permission) }
        sendEvent(NotificationsEvent.RequestPermission(permission))
    }

    fun onPermissionResult(permission: Permission, allowance: AllowanceResult) {
        if (currentViewState.permissionRequestInProgress != permission) return

        permissionCheckJob?.cancel()
        val isAllowed = allowance == AllowanceResult.Allowed
        val bluetoothJustAllowed = permission.affectsBluetooth() && isAllowed &&
                currentViewState.allowedPermissions[permission] != true
        updateState {
            it.copy(
                allowedPermissions = it.allowedPermissions + (permission to isAllowed),
                permissionRequestInProgress = null,
                restrictedPermission = if (isAllowed) null else permission,
            )
        }
        if (bluetoothJustAllowed) sendEvent(NotificationsEvent.BluetoothAllowed)
    }

    fun onOpenSettingsClicked() {
        onDismissDialog()
        permissionsManager.openAppSettings()
    }

    fun onDismissDialog() {
        updateState { it.copy(restrictedPermission = null) }
    }
}

private fun Permission.affectsBluetooth(): Boolean {
    return this == Permission.Bluetooth || this == Permission.BluetoothBackgroundLocation
}
