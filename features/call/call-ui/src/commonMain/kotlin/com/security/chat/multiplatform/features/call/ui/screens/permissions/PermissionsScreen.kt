package com.security.chat.multiplatform.features.call.ui.screens.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogDescriptor
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.call.component.api.PermissionsComponent
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import securitychat.common.icons_kit.generated.resources.ic_back
import securitychat.common.localization.generated.resources.common_cancel
import securitychat.common.localization.generated.resources.permissions_do_not_grant
import securitychat.common.localization.generated.resources.permissions_grant
import securitychat.common.localization.generated.resources.permissions_local_network_description
import securitychat.common.localization.generated.resources.permissions_local_network_restricted_message
import securitychat.common.localization.generated.resources.permissions_local_network_restricted_title
import securitychat.common.localization.generated.resources.permissions_microphone_description
import securitychat.common.localization.generated.resources.permissions_microphone_restricted_message
import securitychat.common.localization.generated.resources.permissions_microphone_restricted_title
import securitychat.common.localization.generated.resources.permissions_open_settings
import securitychat.common.localization.generated.resources.permissions_title

@Composable
internal fun PermissionsScreen(
    component: PermissionsComponent,
) {
    Screen(
        component = component,
        screenName = "PermissionsScreen",
    ) { state: PermissionsState, vm: PermissionsViewModel ->
        val microphoneLauncher = createLauncher(
            permission = Permission.RecordAudio,
            onResult = { vm.onPermissionResult(Permission.RecordAudio, it) },
        )
        val localNetworkLauncher = createLauncher(
            permission = Permission.LocalNetwork,
            onResult = { vm.onPermissionResult(Permission.LocalNetwork, it) },
        )
        val launchers = mapOf(
            Permission.RecordAudio to microphoneLauncher,
            Permission.LocalNetwork to localNetworkLauncher,
        )

        SingleEventEffect(
            sideEffectFlow = vm.viewEvent,
        ) { event ->
            when (event) {
                is PermissionsEvent.RequestPermission ->
                    launchers.getValue(event.permission).request()
            }
        }

        PermissionsContent(
            modifier = Modifier.fillMaxSize(),
            state = state,
            onBackClicked = component::onBackClicked,
            onAllowClicked = vm::onAllowClicked,
            onDenyClicked = vm::showPermissionRestrictedDialog,
        )
    }
}

@Composable
private fun PermissionsContent(
    modifier: Modifier,
    state: PermissionsState,
    onBackClicked: () -> Unit,
    onAllowClicked: () -> Unit,
    onDenyClicked: () -> Unit,
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
                    text = stringResource(StringRes.permissions_title),
                ),
            )
            if (state.isPermissionRequired) {
                val descriptions = listOfNotNull(
                    if (Permission.RecordAudio in state.requiredPermissions) {
                        stringResource(StringRes.permissions_microphone_description)
                    } else null,
                    if (Permission.LocalNetwork in state.requiredPermissions) {
                        stringResource(StringRes.permissions_local_network_description)
                    } else null,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = MAX_CONTENT_WIDTH_DP.dp)
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 16.dp),
                ) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        text = descriptions.joinToString("\n\n"),
                        color = AppTheme.colors.textPrimary,
                        style = AppTheme.typography.body,
                    )
                    Spacer(Modifier.height(24.dp))
                    ButtonPrimary(
                        modifier = Modifier.fillMaxWidth(),
                        content = ButtonContent.Text(
                            text = stringResource(StringRes.permissions_grant),
                        ),
                        onClicked = onAllowClicked,
                    )
                    Spacer(Modifier.height(24.dp))
                    ButtonPrimary(
                        modifier = Modifier.fillMaxWidth(),
                        content = ButtonContent.Text(
                            text = stringResource(StringRes.permissions_do_not_grant),
                        ),
                        onClicked = onDenyClicked,
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        state.alertDialogDescriptor?.let { descriptor ->
            AlertDialogComponent(
                content = descriptor.content,
                onDismissRequest = descriptor.dismissAction,
                onPositiveButtonClicked = descriptor.positiveAction,
                onNegativeButtonClicked = descriptor.negativeAction,
                hazeState = hazeState,
            )
        }
    }
}

@Preview
@Composable
internal fun PermissionsScreenPreview() {
    AppTheme {
        PermissionsContent(
            modifier = Modifier.fillMaxSize(),
            state = PermissionsState(
                requiredPermissions = persistentListOf(Permission.RecordAudio),
                hasNavigatedToCall = false,
                alertDialogDescriptor = null,
            ),
            onBackClicked = {},
            onAllowClicked = {},
            onDenyClicked = {},
        )
    }
}

@Preview
@Composable
internal fun PermissionsScreenRestrictedPreview() {
    AppTheme {
        PermissionsContent(
            modifier = Modifier.fillMaxSize(),
            state = PermissionsState(
                requiredPermissions = persistentListOf(Permission.RecordAudio),
                hasNavigatedToCall = false,
                alertDialogDescriptor = AlertDialogDescriptor(
                    content = AlertDialogContent(
                        title = resPrintableText(StringRes.permissions_microphone_restricted_title),
                        message = resPrintableText(
                            StringRes.permissions_microphone_restricted_message,
                        ),
                        positiveButtonText = resPrintableText(
                            StringRes.permissions_open_settings,
                        ),
                        negativeButtonText = resPrintableText(StringRes.common_cancel),
                    ),
                    dismissAction = {},
                    positiveAction = {},
                    negativeAction = {},
                ),
            ),
            onBackClicked = {},
            onAllowClicked = {},
            onDenyClicked = {},
        )
    }
}

@Preview
@Composable
internal fun PermissionsScreenMicrophoneAndLocalNetworkPreview() {
    AppTheme {
        PermissionsContent(
            modifier = Modifier.fillMaxSize(),
            state = PermissionsState(
                requiredPermissions = persistentListOf(
                    Permission.RecordAudio,
                    Permission.LocalNetwork,
                ),
                hasNavigatedToCall = false,
                alertDialogDescriptor = null,
            ),
            onBackClicked = {},
            onAllowClicked = {},
            onDenyClicked = {},
        )
    }
}

@Preview
@Composable
internal fun PermissionsScreenLocalNetworkPreview() {
    AppTheme {
        PermissionsContent(
            modifier = Modifier.fillMaxSize(),
            state = PermissionsState(
                requiredPermissions = persistentListOf(Permission.LocalNetwork),
                hasNavigatedToCall = false,
                alertDialogDescriptor = null,
            ),
            onBackClicked = {},
            onAllowClicked = {},
            onDenyClicked = {},
        )
    }
}

@Preview
@Composable
internal fun PermissionsScreenLocalNetworkRestrictedPreview() {
    AppTheme {
        PermissionsContent(
            modifier = Modifier.fillMaxSize(),
            state = PermissionsState(
                requiredPermissions = persistentListOf(Permission.LocalNetwork),
                hasNavigatedToCall = false,
                alertDialogDescriptor = AlertDialogDescriptor(
                    content = AlertDialogContent(
                        title = resPrintableText(
                            StringRes.permissions_local_network_restricted_title,
                        ),
                        message = resPrintableText(
                            StringRes.permissions_local_network_restricted_message,
                        ),
                        positiveButtonText = resPrintableText(
                            StringRes.permissions_open_settings,
                        ),
                        negativeButtonText = resPrintableText(StringRes.common_cancel),
                    ),
                    dismissAction = {},
                    positiveAction = {},
                    negativeAction = {},
                ),
            ),
            onBackClicked = {},
            onAllowClicked = {},
            onDenyClicked = {},
        )
    }
}
