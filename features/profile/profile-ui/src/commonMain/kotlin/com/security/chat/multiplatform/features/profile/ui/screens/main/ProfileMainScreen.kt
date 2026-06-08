package com.security.chat.multiplatform.features.profile.ui.screens.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.blur
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.toast.ToastHost
import com.mobilebytelabs.kmptoolkit.toast.rememberToastHostState
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.core.ui.SingleEventEffect
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState
import com.security.chat.multiplatform.common.core.ui.entity.resolveNoCompose
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import securitychat.common.icons_kit.generated.resources.ic_back
import securitychat.common.localization.generated.resources.profile_copy_private_key
import securitychat.common.localization.generated.resources.profile_hide_private_key
import securitychat.common.localization.generated.resources.profile_login_label
import securitychat.common.localization.generated.resources.profile_login_placeholder
import securitychat.common.localization.generated.resources.profile_section_private_key
import securitychat.common.localization.generated.resources.profile_show_private_key
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
            events = vm.viewEvent,
            onBackClicked = component::onBackClicked,
            onUsernameTextChanged = vm::onUsernameTextChanged,
            onUpdateUsernameClicked = vm::onUpdateUsernameClicked,
            onTogglePrivateKeyVisibilityClicked = vm::onTogglePrivateKeyVisibilityClicked,
            onCopyPrivateKeyClicked = vm::onCopyPrivateKeyClicked,
        )
    }
}

@Composable
private fun ProfileMainScreenContent(
    modifier: Modifier,
    state: ProfileMainState,
    events: Flow<ProfileMainEvent>,
    onBackClicked: () -> Unit,
    onUsernameTextChanged: (String) -> Unit,
    onUpdateUsernameClicked: () -> Unit,
    onTogglePrivateKeyVisibilityClicked: () -> Unit,
    onCopyPrivateKeyClicked: () -> Unit,
) {
    val toastState = rememberToastHostState()
    val scope = rememberCoroutineScope()

    SingleEventEffect(
        sideEffectFlow = events,
        collector = { event ->
            when (event) {
                is ProfileMainEvent.Toast -> {
                    scope.launch {
                        toastState.showToast(event.text.resolveNoCompose())
                    }
                }
            }
        },
    )

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
                onTogglePrivateKeyVisibilityClicked = onTogglePrivateKeyVisibilityClicked,
                onCopyPrivateKeyClicked = onCopyPrivateKeyClicked,
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
        ToastHost(hostState = toastState)
    }
}

@Composable
private fun ProfileContent(
    modifier: Modifier = Modifier,
    state: ProfileMainState,
    onUsernameTextChanged: (String) -> Unit,
    onUpdateUsernameClicked: () -> Unit,
    onTogglePrivateKeyVisibilityClicked: () -> Unit,
    onCopyPrivateKeyClicked: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(StringRes.profile_login_label),
                color = AppTheme.colors.textPrimary,
                style = AppTheme.typography.title2,
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentType = ContentType.Username },
                value = state.login,
                onValueChange = onUsernameTextChanged,
                placeholder = {
                    Text(text = stringResource(StringRes.profile_login_placeholder))
                },
                enabled = !state.showLoading,
            )
        }
        Spacer(Modifier.height(40.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = stringResource(StringRes.profile_section_private_key),
                color = AppTheme.colors.textPrimary,
                style = AppTheme.typography.title2,
            )
            Spacer(Modifier.height(8.dp))
            val animatedBlur: Dp by animateDpAsState(
                targetValue = if (state.isPrivateKeyHidden) 10.dp else 0.dp,
                label = "animation_blur",
                animationSpec = tween(
                    durationMillis = 300,
                    easing = LinearEasing,
                ),
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = AppTheme.colors.backgroundSecondary,
                        shape = AppTheme.shapes.roundedRectangle16,
                    )
                    .blur(radius = animatedBlur)
                    .padding(8.dp),
                text = state.privateKey,
                color = AppTheme.colors.textSecondary,
                style = AppTheme.typography.body,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                val togglePrivateKeyVisibilityText = if (state.isPrivateKeyHidden) {
                    stringResource(StringRes.profile_show_private_key)
                } else {
                    stringResource(StringRes.profile_hide_private_key)
                }
                ButtonPrimary(
                    content = ButtonContent.Text(togglePrivateKeyVisibilityText),
                    onClicked = onTogglePrivateKeyVisibilityClicked,
                )
                Spacer(Modifier.weight(1f))
                ButtonPrimary(
                    content = ButtonContent.Text(stringResource(StringRes.profile_copy_private_key)),
                    onClicked = onCopyPrivateKeyClicked,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
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
                privateKey = "privateKey",
                isPrivateKeyHidden = true,
            ),
            events = emptyFlow(),
            onBackClicked = {},
            onUsernameTextChanged = {},
            onUpdateUsernameClicked = {},
            onTogglePrivateKeyVisibilityClicked = {},
            onCopyPrivateKeyClicked = {},
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
                privateKey = "privateKey",
                isPrivateKeyHidden = true,
            ),
            events = emptyFlow(),
            onBackClicked = {},
            onUsernameTextChanged = {},
            onUpdateUsernameClicked = {},
            onTogglePrivateKeyVisibilityClicked = {},
            onCopyPrivateKeyClicked = {},
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
                privateKey = "privateKey",
                isPrivateKeyHidden = true,
            ),
            events = emptyFlow(),
            onBackClicked = {},
            onUsernameTextChanged = {},
            onUpdateUsernameClicked = {},
            onTogglePrivateKeyVisibilityClicked = {},
            onCopyPrivateKeyClicked = {},
        )
    }
}
