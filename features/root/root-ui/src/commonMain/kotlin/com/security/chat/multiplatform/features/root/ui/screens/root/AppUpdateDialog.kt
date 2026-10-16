package com.security.chat.multiplatform.features.root.ui.screens.root

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogComponent
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.stringResource
import securitychat.common.localization.generated.resources.app_update_later
import securitychat.common.localization.generated.resources.app_update_ready_message
import securitychat.common.localization.generated.resources.app_update_ready_title
import securitychat.common.localization.generated.resources.app_update_restart

@Composable
internal fun AppUpdateDialog(
    hazeState: HazeState,
    onRestart: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialogComponent(
        title = stringResource(StringRes.app_update_ready_title),
        message = stringResource(StringRes.app_update_ready_message),
        positiveButtonText = stringResource(StringRes.app_update_restart),
        negativeButtonText = stringResource(StringRes.app_update_later),
        hazeState = hazeState,
        onDismissRequest = onDismiss,
        onPositiveButtonClicked = onRestart,
        onNegativeButtonClicked = onDismiss,
    )
}

@Preview
@Composable
internal fun AppUpdateDialogPreview() {
    AppTheme {
        AppUpdateDialog(
            hazeState = rememberHazeState(),
            onRestart = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
internal fun AppUpdateDialogDarkPreview() {
    AppTheme(useDarkTheme = true) {
        AppUpdateDialog(
            hazeState = rememberHazeState(),
            onRestart = {},
            onDismiss = {},
        )
    }
}
