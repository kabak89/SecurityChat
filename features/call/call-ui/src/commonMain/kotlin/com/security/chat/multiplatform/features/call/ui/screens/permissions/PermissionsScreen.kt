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
import org.jetbrains.compose.resources.stringResource
import securitychat.common.icons_kit.generated.resources.ic_back
import securitychat.common.localization.generated.resources.call_permission_description
import securitychat.common.localization.generated.resources.call_permission_do_not_grant
import securitychat.common.localization.generated.resources.call_permission_grant
import securitychat.common.localization.generated.resources.call_permission_open_settings
import securitychat.common.localization.generated.resources.call_permission_restricted_message
import securitychat.common.localization.generated.resources.call_permission_restricted_title
import securitychat.common.localization.generated.resources.call_permissions_title
import securitychat.common.localization.generated.resources.common_cancel

@Composable
internal fun PermissionsScreen(
    component: PermissionsComponent,
) {
    Screen(
        component = component,
        screenName = "PermissionsScreen",
    ) { state: PermissionsState, vm: PermissionsViewModel ->
        val permissionLauncher = createLauncher(
            permission = Permission.RecordAudio,
            onResult = vm::onRecordAudioPermissionResult,
        )

        SingleEventEffect(
            sideEffectFlow = vm.viewEvent,
        ) { event ->
            when (event) {
                PermissionsEvent.RequestRecordAudioPermission -> permissionLauncher.request()
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
                    text = stringResource(StringRes.call_permissions_title),
                ),
            )
            if (state.isPermissionRequired) {
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
                        text = stringResource(StringRes.call_permission_description),
                        color = AppTheme.colors.textPrimary,
                        style = AppTheme.typography.body,
                    )
                    Spacer(Modifier.height(24.dp))
                    ButtonPrimary(
                        modifier = Modifier.fillMaxWidth(),
                        content = ButtonContent.Text(
                            text = stringResource(StringRes.call_permission_grant),
                        ),
                        onClicked = onAllowClicked,
                    )
                    Spacer(Modifier.height(24.dp))
                    ButtonPrimary(
                        modifier = Modifier.fillMaxWidth(),
                        content = ButtonContent.Text(
                            text = stringResource(StringRes.call_permission_do_not_grant),
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
                isPermissionRequired = true,
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
                isPermissionRequired = true,
                hasNavigatedToCall = false,
                alertDialogDescriptor = AlertDialogDescriptor(
                    content = AlertDialogContent(
                        title = resPrintableText(StringRes.call_permission_restricted_title),
                        message = resPrintableText(StringRes.call_permission_restricted_message),
                        positiveButtonText = resPrintableText(
                            StringRes.call_permission_open_settings,
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
