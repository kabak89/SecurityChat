package com.security.chat.multiplatform.features.onboarding.ui.screens.notificationpermission

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.core.ui.SingleEventEffect
import com.security.chat.multiplatform.common.permission.bluetoothScanRequiresLocation
import com.security.chat.multiplatform.common.permission.createLauncher
import com.security.chat.multiplatform.common.permission.entity.AllowanceResult
import com.security.chat.multiplatform.common.permission.entity.Permission
import com.security.chat.multiplatform.common.ui.kit.MAX_CONTENT_WIDTH_DP
import com.security.chat.multiplatform.common.ui.kit.components.ButtonContent
import com.security.chat.multiplatform.common.ui.kit.components.ButtonPrimary
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogComponent
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.onboarding.component.api.OnboardingMainComponent
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.stringResource
import securitychat.common.localization.generated.resources.notification_permission_bluetooth_background_description
import securitychat.common.localization.generated.resources.notification_permission_bluetooth_background_do_not_grant
import securitychat.common.localization.generated.resources.notification_permission_bluetooth_background_grant
import securitychat.common.localization.generated.resources.notification_permission_bluetooth_description
import securitychat.common.localization.generated.resources.notification_permission_bluetooth_do_not_grant
import securitychat.common.localization.generated.resources.notification_permission_bluetooth_grant
import securitychat.common.localization.generated.resources.notification_permission_bluetooth_location_description
import securitychat.common.localization.generated.resources.notification_permission_bluetooth_location_grant
import securitychat.common.localization.generated.resources.notification_permission_description
import securitychat.common.localization.generated.resources.notification_permission_do_not_grant
import securitychat.common.localization.generated.resources.notification_permission_grant

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NotificationPermissionScreen(
    component: OnboardingMainComponent,
) {
    Screen(
        component = component,
        screenName = "NotificationPermissionScreen",
    ) { state: NotificationPermissionState, vm: NotificationPermissionViewModel ->
        NotificationPermissionScreenContent(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            state = state,
            locationRequiredForBluetoothScan = bluetoothScanRequiresLocation(),
            events = vm.viewEvent,
            close = component::onFinish,
            onPermissionResult = vm::onPermissionResult,
        )
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = true,
        onBackCompleted = component::onFinish,
    )
}

@Preview
@Composable
internal fun NotificationPermissionScreenPreview() {
    PermissionScreenPreview(permission = Permission.Notifications)
}

@Preview
@Composable
internal fun BluetoothPermissionScreenPreview() {
    PermissionScreenPreview(permission = Permission.Bluetooth)
}

@Preview
@Composable
internal fun BluetoothLocationPermissionScreenPreview() {
    PermissionScreenPreview(
        permission = Permission.Bluetooth,
        locationRequiredForBluetoothScan = true,
    )
}

@Preview
@Composable
internal fun BluetoothBackgroundPermissionScreenPreview() {
    PermissionScreenPreview(permission = Permission.BluetoothBackgroundLocation)
}

@Composable
private fun NotificationPermissionScreenContent(
    modifier: Modifier,
    state: NotificationPermissionState,
    locationRequiredForBluetoothScan: Boolean,
    events: Flow<NotificationPermissionEvent>,
    close: () -> Unit,
    onPermissionResult: (Permission, AllowanceResult) -> Unit,
) {
    SingleEventEffect(
        sideEffectFlow = events,
        collector = { event ->
            when (event) {
                NotificationPermissionEvent.Finished -> close()
            }
        },
    )
    val hazeState = rememberHazeState()
    Box(
        modifier = modifier
            .background(AppTheme.colors.backgroundPrimary)
            .fillMaxSize()
            .systemBarsPadding()
            .hazeSource(hazeState),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = MAX_CONTENT_WIDTH_DP.dp)
                .align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
        ) {
            val permission = state.permission ?: return@Column
            val isBluetooth = permission == Permission.Bluetooth
            val isBackgroundLocation = permission == Permission.BluetoothBackgroundLocation
            val requestsLocation = isBluetooth && locationRequiredForBluetoothScan
            Spacer(Modifier.height(24.dp))
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                text = stringResource(
                    when {
                        isBackgroundLocation -> StringRes.notification_permission_bluetooth_background_description
                        requestsLocation -> StringRes.notification_permission_bluetooth_location_description
                        isBluetooth -> StringRes.notification_permission_bluetooth_description
                        else -> StringRes.notification_permission_description
                    },
                ),
                color = AppTheme.colors.textPrimary,
                style = AppTheme.typography.body,
            )
            Spacer(Modifier.height(24.dp))
            val permissionLauncher = createLauncher(
                permission = permission,
                onResult = { allowance -> onPermissionResult(permission, allowance) },
            )
            ButtonPrimary(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                content = ButtonContent.Text(
                    text = stringResource(
                        when {
                            isBackgroundLocation -> StringRes.notification_permission_bluetooth_background_grant
                            requestsLocation -> StringRes.notification_permission_bluetooth_location_grant
                            isBluetooth -> StringRes.notification_permission_bluetooth_grant
                            else -> StringRes.notification_permission_grant
                        },
                    ),
                ),
                onClicked = { permissionLauncher.request() },
            )
            Spacer(Modifier.height(24.dp))
            ButtonPrimary(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                content = ButtonContent.Text(
                    text = stringResource(
                        when {
                            isBackgroundLocation -> StringRes.notification_permission_bluetooth_background_do_not_grant
                            isBluetooth -> StringRes.notification_permission_bluetooth_do_not_grant
                            else -> StringRes.notification_permission_do_not_grant
                        },
                    ),
                ),
                onClicked = { onPermissionResult(permission, AllowanceResult.Restricted) },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
    val alertDialogDescriptor = state.alertDialogDescriptor
    if (alertDialogDescriptor != null) {
        AlertDialogComponent(
            content = alertDialogDescriptor.content,
            onDismissRequest = alertDialogDescriptor.dismissAction,
            onPositiveButtonClicked = alertDialogDescriptor.positiveAction,
            onNegativeButtonClicked = alertDialogDescriptor.negativeAction,
            hazeState = hazeState,
        )
    }
}

@Composable
private fun PermissionScreenPreview(
    permission: Permission,
    locationRequiredForBluetoothScan: Boolean = false,
) {
    AppTheme {
        NotificationPermissionScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = NotificationPermissionState(
                permission = permission,
                alertDialogDescriptor = null,
            ),
            locationRequiredForBluetoothScan = locationRequiredForBluetoothScan,
            events = emptyFlow(),
            close = {},
            onPermissionResult = { _, _ -> },
        )
    }
}
