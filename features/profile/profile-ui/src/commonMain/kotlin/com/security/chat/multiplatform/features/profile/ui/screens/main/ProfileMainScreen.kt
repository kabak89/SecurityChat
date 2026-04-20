package com.security.chat.multiplatform.features.profile.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.MAX_CONTENT_WIDTH_DP
import com.security.chat.multiplatform.common.ui.kit.components.ButtonContent
import com.security.chat.multiplatform.common.ui.kit.components.ButtonPrimary
import com.security.chat.multiplatform.common.ui.kit.components.CenterContent
import com.security.chat.multiplatform.common.ui.kit.components.SideContent
import com.security.chat.multiplatform.common.ui.kit.components.ToolbarComponent
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogComponent
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.profile.component.api.ProfileMainComponent
import org.jetbrains.compose.resources.stringResource
import securitychat.common.icons_kit.generated.resources.ic_back
import securitychat.common.localization.generated.resources.profile_login_label
import securitychat.common.localization.generated.resources.profile_login_placeholder
import securitychat.common.localization.generated.resources.profile_title
import securitychat.common.localization.generated.resources.profile_update

@Composable
internal fun ProfileMainScreen(
    component: ProfileMainComponent,
) {
    Screen(component) { state: ProfileMainState, vm: ProfileMainViewModel ->
        ProfileMainScreenContent(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            state = state,
            onBackClicked = component::onBackClicked,
            onUsernameTextChanged = vm::onUsernameTextChanged,
            onUpdateUsernameClicked = vm::onUpdateUsernameClicked,
        )
    }
}

@Composable
private fun ProfileMainScreenContent(
    modifier: Modifier,
    state: ProfileMainState,
    onBackClicked: () -> Unit,
    onUsernameTextChanged: (String) -> Unit,
    onUpdateUsernameClicked: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(AppTheme.colors.backgroundPrimary)
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            ToolbarComponent(
                modifier = Modifier
                    .fillMaxWidth(),
                startContent = SideContent.Button(
                    icon = DrawableRes.ic_back,
                    onClicked = onBackClicked,
                ),
                centerContent = CenterContent.Title(
                    text = stringResource(StringRes.profile_title),
                ),
                endContent = null,
            )
            ProfileContent(
                modifier = Modifier
                    .widthIn(max = MAX_CONTENT_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                state = state,
                onUsernameTextChanged = onUsernameTextChanged,
                onUpdateUsernameClicked = onUpdateUsernameClicked,
            )
        }
        if (state.dialogContent != null) {
            AlertDialogComponent(
                content = state.dialogContent.errorDialogContent,
                onDismissRequest = state.dialogContent.dismissAction,
                onPositiveButtonClicked = state.dialogContent.positiveAction,
                onNegativeButtonClicked = state.dialogContent.negativeAction,
            )
        }
    }
}

@Composable
private fun ProfileContent(
    modifier: Modifier = Modifier,
    state: ProfileMainState,
    onUsernameTextChanged: (String) -> Unit,
    onUpdateUsernameClicked: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(StringRes.profile_login_label),
                color = AppTheme.colors.textPrimary,
                style = AppTheme.typography.title2,
            )
            Spacer(modifier = Modifier.width(16.dp))
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .semantics { contentType = ContentType.Username },
                value = state.login,
                onValueChange = onUsernameTextChanged,
                placeholder = {
                    Text(text = stringResource(StringRes.profile_login_placeholder))
                },
                enabled = !state.showLoading,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (state.showLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(48.dp),
                color = AppTheme.colors.element,
            )
        } else {
            ButtonPrimary(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                content = ButtonContent.Text(text = stringResource(StringRes.profile_update)),
                enabled = state.loginChangeEnabled,
                onClicked = onUpdateUsernameClicked,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview
@Composable
internal fun ProfileMainScreenPreview() {
    AppTheme {
        ProfileMainScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = ProfileMainState(
                fetchInfoState = UiLceState.Ready,
                login = "user_1",
                changeNameState = UiLceState.Ready,
                loginChangeEnabled = true,
                dialogContent = null,
            ),
            onBackClicked = {},
            onUsernameTextChanged = {},
            onUpdateUsernameClicked = {},
        )
    }
}

@Preview
@Composable
internal fun ProfileMainScreenPreviewUpdateDisabled() {
    AppTheme {
        ProfileMainScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = ProfileMainState(
                fetchInfoState = UiLceState.Ready,
                login = "user_1",
                changeNameState = UiLceState.Ready,
                loginChangeEnabled = false,
                dialogContent = null,
            ),
            onBackClicked = {},
            onUsernameTextChanged = {},
            onUpdateUsernameClicked = {},
        )
    }
}

@Preview
@Composable
internal fun ProfileMainScreenPreviewLoading() {
    AppTheme {
        ProfileMainScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = ProfileMainState(
                fetchInfoState = UiLceState.Ready,
                login = "user_1",
                changeNameState = UiLceState.Loading,
                loginChangeEnabled = true,
                dialogContent = null,
            ),
            onBackClicked = {},
            onUsernameTextChanged = {},
            onUpdateUsernameClicked = {},
        )
    }
}
