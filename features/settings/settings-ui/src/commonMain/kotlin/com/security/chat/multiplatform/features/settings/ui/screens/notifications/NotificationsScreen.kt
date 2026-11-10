package com.security.chat.multiplatform.features.settings.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.core.ui.SingleEventEffect
import com.security.chat.multiplatform.common.core.ui.entity.resPrintableText
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.permission.bluetoothScanRequiresLocation
import com.security.chat.multiplatform.common.permission.createLauncher
import com.security.chat.multiplatform.common.permission.entity.Permission
import com.security.chat.multiplatform.common.ui.kit.MAX_CONTENT_WIDTH_DP
import com.security.chat.multiplatform.common.ui.kit.components.ButtonContent
import com.security.chat.multiplatform.common.ui.kit.components.ButtonPrimary
import com.security.chat.multiplatform.common.ui.kit.components.CenterContent
import com.security.chat.multiplatform.common.ui.kit.components.SideContent
import com.security.chat.multiplatform.common.ui.kit.components.ToolbarComponent
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogComponent
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogContent
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.settings.component.api.NotificationsComponent
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.stringResource
import securitychat.common.icons_kit.generated.resources.ic_back
import securitychat.common.localization.generated.resources.common_cancel
import securitychat.common.localization.generated.resources.notifications_allowed
import securitychat.common.localization.generated.resources.notifications_bluetooth_allowed
import securitychat.common.localization.generated.resources.notifications_bluetooth_background_allowed
import securitychat.common.localization.generated.resources.notifications_bluetooth_background_description
import securitychat.common.localization.generated.resources.notifications_bluetooth_background_grant
import securitychat.common.localization.generated.resources.notifications_bluetooth_background_not_allowed
import securitychat.common.localization.generated.resources.notifications_bluetooth_background_title
import securitychat.common.localization.generated.resources.notifications_bluetooth_description
import securitychat.common.localization.generated.resources.notifications_bluetooth_grant
import securitychat.common.localization.generated.resources.notifications_bluetooth_location_allowed
import securitychat.common.localization.generated.resources.notifications_bluetooth_location_description
import securitychat.common.localization.generated.resources.notifications_bluetooth_location_grant
import securitychat.common.localization.generated.resources.notifications_bluetooth_location_not_allowed
import securitychat.common.localization.generated.resources.notifications_bluetooth_location_restricted_message
import securitychat.common.localization.generated.resources.notifications_bluetooth_location_restricted_title
import securitychat.common.localization.generated.resources.notifications_bluetooth_not_allowed
import securitychat.common.localization.generated.resources.notifications_bluetooth_restricted_message
import securitychat.common.localization.generated.resources.notifications_bluetooth_restricted_title
import securitychat.common.localization.generated.resources.notifications_bluetooth_title
import securitychat.common.localization.generated.resources.notifications_description
import securitychat.common.localization.generated.resources.notifications_grant
import securitychat.common.localization.generated.resources.notifications_not_allowed
import securitychat.common.localization.generated.resources.notifications_open_settings
import securitychat.common.localization.generated.resources.notifications_restricted_message
import securitychat.common.localization.generated.resources.notifications_restricted_title
import securitychat.common.localization.generated.resources.notifications_section_title
import securitychat.common.localization.generated.resources.notifications_title

@Composable
internal fun NotificationsScreen(
    component: NotificationsComponent,
) {
    Screen(
        component = component,
        screenName = "NotificationsScreen",
    ) { state: NotificationsState, vm: NotificationsViewModel ->
        val notificationsLauncher = createLauncher(
            permission = Permission.Notifications,
            onResult = { vm.onPermissionResult(Permission.Notifications, it) },
        )
        val bluetoothLauncher = if (Permission.Bluetooth in state.permissions) {
            createLauncher(
                permission = Permission.Bluetooth,
                onResult = { vm.onPermissionResult(Permission.Bluetooth, it) },
            )
        } else {
            null
        }
        val backgroundLocationLauncher =
            if (Permission.BluetoothBackgroundLocation in state.permissions) {
                createLauncher(
                    permission = Permission.BluetoothBackgroundLocation,
                    onResult = {
                        vm.onPermissionResult(
                            Permission.BluetoothBackgroundLocation,
                            it,
                        )
                    },
                )
            } else {
                null
            }
        SingleEventEffect(sideEffectFlow = vm.viewEvent) { event ->
            when (event) {
                is NotificationsEvent.RequestPermission -> {
                    when (event.permission) {
                        Permission.Notifications -> notificationsLauncher.request()
                        Permission.Bluetooth -> bluetoothLauncher?.request()
                        Permission.BluetoothBackgroundLocation -> backgroundLocationLauncher?.request()
                        else -> Unit
                    }
                }

                NotificationsEvent.BluetoothAllowed -> component.onBluetoothAllowed()
            }
        }
        NotificationsScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = state,
            locationRequiredForBluetoothScan = bluetoothScanRequiresLocation(),
            onBackClicked = component::onBackClicked,
            onAllowClicked = vm::onAllowClicked,
            onOpenSettingsClicked = vm::onOpenSettingsClicked,
            onDismissDialog = vm::onDismissDialog,
        )
    }
}

@Preview
@Composable
internal fun NotificationsScreenPreview() {
    PermissionsPreview(isAllowed = false, restrictedPermission = null)
}

@Preview
@Composable
internal fun NotificationsScreenAllowedPreview() {
    PermissionsPreview(isAllowed = true, restrictedPermission = null)
}

@Preview
@Composable
internal fun NotificationsScreenRestrictedPreview() {
    PermissionsPreview(isAllowed = false, restrictedPermission = Permission.Bluetooth)
}

@Preview
@Composable
internal fun NotificationsScreenBluetoothLocationPreview() {
    PermissionsPreview(
        isAllowed = false,
        restrictedPermission = null,
        locationRequiredForBluetoothScan = true,
    )
}

@Preview
@Composable
internal fun NotificationsScreenBluetoothBackgroundPreview() {
    PermissionsPreview(
        isAllowed = false,
        restrictedPermission = null,
        locationRequiredForBluetoothScan = true,
        showBackgroundLocation = true,
    )
}

@Composable
private fun NotificationsScreenContent(
    modifier: Modifier,
    state: NotificationsState,
    locationRequiredForBluetoothScan: Boolean,
    onBackClicked: () -> Unit,
    onAllowClicked: (Permission) -> Unit,
    onOpenSettingsClicked: () -> Unit,
    onDismissDialog: () -> Unit,
) {
    val hazeState = rememberHazeState()
    Box(
        modifier = modifier.background(AppTheme.colors.backgroundPrimary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .hazeSource(hazeState),
        ) {
            ToolbarComponent(
                modifier = Modifier.fillMaxWidth(),
                startContent = SideContent.Button(
                    icon = DrawableRes.ic_back,
                    onClicked = onBackClicked,
                ),
                centerContent = CenterContent.Title(
                    text = stringResource(StringRes.notifications_title),
                ),
                endContent = null,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = MAX_CONTENT_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                state.permissions.forEach { permission ->
                    if (permission == Permission.BluetoothBackgroundLocation &&
                        state.allowedPermissions[Permission.Bluetooth] != true
                    ) {
                        return@forEach
                    }
                    PermissionSection(
                        permission = permission,
                        state = state,
                        locationRequiredForBluetoothScan = locationRequiredForBluetoothScan,
                        onAllowClicked = { onAllowClicked(permission) },
                    )
                }
            }
        }
        state.restrictedPermission?.let { permission ->
            val isBluetooth = permission == Permission.Bluetooth
            val isBackgroundLocation = permission == Permission.BluetoothBackgroundLocation
            val requestsLocation = isBluetooth && locationRequiredForBluetoothScan
            AlertDialogComponent(
                content = AlertDialogContent(
                    title = resPrintableText(
                        when {
                            isBackgroundLocation -> StringRes.notifications_bluetooth_background_title
                            requestsLocation -> StringRes.notifications_bluetooth_location_restricted_title
                            isBluetooth -> StringRes.notifications_bluetooth_restricted_title
                            else -> StringRes.notifications_restricted_title
                        },
                    ),
                    message = resPrintableText(
                        when {
                            isBackgroundLocation -> StringRes.notifications_bluetooth_background_description
                            requestsLocation -> StringRes.notifications_bluetooth_location_restricted_message
                            isBluetooth -> StringRes.notifications_bluetooth_restricted_message
                            else -> StringRes.notifications_restricted_message
                        },
                    ),
                    positiveButtonText = resPrintableText(StringRes.notifications_open_settings),
                    negativeButtonText = resPrintableText(StringRes.common_cancel),
                ),
                onDismissRequest = onDismissDialog,
                onPositiveButtonClicked = onOpenSettingsClicked,
                onNegativeButtonClicked = onDismissDialog,
                hazeState = hazeState,
            )
        }
    }
}

@Composable
private fun PermissionSection(
    permission: Permission,
    state: NotificationsState,
    locationRequiredForBluetoothScan: Boolean,
    onAllowClicked: () -> Unit,
) {
    val isBluetooth = permission == Permission.Bluetooth
    val isBackgroundLocation = permission == Permission.BluetoothBackgroundLocation
    val requestsLocation = isBluetooth && locationRequiredForBluetoothScan
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(
                when {
                    isBackgroundLocation -> StringRes.notifications_bluetooth_background_title
                    isBluetooth -> StringRes.notifications_bluetooth_title
                    else -> StringRes.notifications_section_title
                },
            ),
            color = AppTheme.colors.textPrimary,
            style = AppTheme.typography.title,
        )
        Text(
            text = stringResource(
                when {
                    isBackgroundLocation -> StringRes.notifications_bluetooth_background_description
                    requestsLocation -> StringRes.notifications_bluetooth_location_description
                    isBluetooth -> StringRes.notifications_bluetooth_description
                    else -> StringRes.notifications_description
                },
            ),
            color = AppTheme.colors.textPrimary,
            style = AppTheme.typography.body,
        )
        val isAllowed = state.allowedPermissions[permission] ?: return@Column
        val status = when {
            isBackgroundLocation && isAllowed -> StringRes.notifications_bluetooth_background_allowed
            isBackgroundLocation -> StringRes.notifications_bluetooth_background_not_allowed
            requestsLocation && isAllowed -> StringRes.notifications_bluetooth_location_allowed
            requestsLocation -> StringRes.notifications_bluetooth_location_not_allowed
            isBluetooth && isAllowed -> StringRes.notifications_bluetooth_allowed
            isBluetooth -> StringRes.notifications_bluetooth_not_allowed
            isAllowed -> StringRes.notifications_allowed
            else -> StringRes.notifications_not_allowed
        }
        Text(
            text = stringResource(status),
            color = AppTheme.colors.textPrimary,
            style = AppTheme.typography.body,
        )
        if (!isAllowed) {
            ButtonPrimary(
                modifier = Modifier.fillMaxWidth(),
                content = ButtonContent.Text(
                    text = stringResource(
                        when {
                            isBackgroundLocation -> StringRes.notifications_bluetooth_background_grant
                            requestsLocation -> StringRes.notifications_bluetooth_location_grant
                            isBluetooth -> StringRes.notifications_bluetooth_grant
                            else -> StringRes.notifications_grant
                        },
                    ),
                ),
                enabled = state.permissionRequestInProgress == null,
                onClicked = onAllowClicked,
            )
        }
    }
}

@Composable
private fun PermissionsPreview(
    isAllowed: Boolean,
    restrictedPermission: Permission?,
    locationRequiredForBluetoothScan: Boolean = false,
    showBackgroundLocation: Boolean = false,
) {
    val permissions = buildList {
        add(Permission.Notifications)
        add(Permission.Bluetooth)
        if (showBackgroundLocation) add(Permission.BluetoothBackgroundLocation)
    }
    AppTheme {
        NotificationsScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = NotificationsState(
                permissions = permissions,
                allowedPermissions = permissions.associateWith {
                    isAllowed || (showBackgroundLocation && it == Permission.Bluetooth)
                },
                permissionRequestInProgress = null,
                restrictedPermission = restrictedPermission,
            ),
            locationRequiredForBluetoothScan = locationRequiredForBluetoothScan,
            onBackClicked = {},
            onAllowClicked = {},
            onOpenSettingsClicked = {},
            onDismissDialog = {},
        )
    }
}
