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
import com.security.chat.multiplatform.features.settings.component.api.NotificationsComponent
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.stringResource
import securitychat.common.icons_kit.generated.resources.ic_back
import securitychat.common.localization.generated.resources.common_cancel
import securitychat.common.localization.generated.resources.notifications_allowed
import securitychat.common.localization.generated.resources.notifications_description
import securitychat.common.localization.generated.resources.notifications_grant
import securitychat.common.localization.generated.resources.notifications_not_allowed
import securitychat.common.localization.generated.resources.notifications_open_settings
import securitychat.common.localization.generated.resources.notifications_restricted_message
import securitychat.common.localization.generated.resources.notifications_restricted_title
import securitychat.common.localization.generated.resources.notifications_title

@Composable
internal fun NotificationsScreen(
    component: NotificationsComponent,
) {
    Screen(
        component = component,
        screenName = "NotificationsScreen",
    ) { state: NotificationsState, vm: NotificationsViewModel ->
        val permissionLauncher = createLauncher(
            permission = Permission.Notifications,
            onResult = vm::onPermissionResult,
        )
        SingleEventEffect(sideEffectFlow = vm.viewEvent) { event ->
            when (event) {
                NotificationsEvent.RequestPermission -> permissionLauncher.request()
            }
        }
        NotificationsScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = state,
            onBackClicked = component::onBackClicked,
            onAllowClicked = vm::onAllowClicked,
        )
    }
}

@Composable
private fun NotificationsScreenContent(
    modifier: Modifier,
    state: NotificationsState,
    onBackClicked: () -> Unit,
    onAllowClicked: () -> Unit,
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
                Text(
                    text = stringResource(StringRes.notifications_description),
                    color = AppTheme.colors.textPrimary,
                    style = AppTheme.typography.body,
                )
                val isAllowed = state.isPermissionAllowed
                if (isAllowed != null) {
                    Text(
                        text = stringResource(
                            if (isAllowed) StringRes.notifications_allowed
                            else StringRes.notifications_not_allowed,
                        ),
                        color = AppTheme.colors.textPrimary,
                        style = AppTheme.typography.body,
                    )
                    if (!isAllowed) {
                        ButtonPrimary(
                            modifier = Modifier.fillMaxWidth(),
                            content = ButtonContent.Text(
                                text = stringResource(StringRes.notifications_grant),
                            ),
                            enabled = !state.permissionRequestInProgress,
                            onClicked = onAllowClicked,
                        )
                    }
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
internal fun NotificationsScreenPreview() {
    AppTheme {
        NotificationsScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = NotificationsState(
                isPermissionAllowed = false,
                permissionRequestInProgress = false,
                alertDialogDescriptor = null,
            ),
            onBackClicked = {},
            onAllowClicked = {},
        )
    }
}

@Preview
@Composable
internal fun NotificationsScreenAllowedPreview() {
    AppTheme {
        NotificationsScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = NotificationsState(
                isPermissionAllowed = true,
                permissionRequestInProgress = false,
                alertDialogDescriptor = null,
            ),
            onBackClicked = {},
            onAllowClicked = {},
        )
    }
}

@Preview
@Composable
internal fun NotificationsScreenRestrictedPreview() {
    AppTheme {
        NotificationsScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = NotificationsState(
                isPermissionAllowed = false,
                permissionRequestInProgress = false,
                alertDialogDescriptor = AlertDialogDescriptor(
                    content = AlertDialogContent(
                        title = resPrintableText(StringRes.notifications_restricted_title),
                        message = resPrintableText(StringRes.notifications_restricted_message),
                        positiveButtonText = resPrintableText(StringRes.notifications_open_settings),
                        negativeButtonText = resPrintableText(StringRes.common_cancel),
                    ),
                    dismissAction = {},
                    positiveAction = {},
                    negativeAction = {},
                ),
            ),
            onBackClicked = {},
            onAllowClicked = {},
        )
    }
}
