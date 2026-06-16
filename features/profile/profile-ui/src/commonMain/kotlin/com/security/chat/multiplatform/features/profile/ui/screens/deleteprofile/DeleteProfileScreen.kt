package com.security.chat.multiplatform.features.profile.ui.screens.deleteprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.MAX_CONTENT_WIDTH_DP
import com.security.chat.multiplatform.common.ui.kit.components.CenterContent
import com.security.chat.multiplatform.common.ui.kit.components.SideContent
import com.security.chat.multiplatform.common.ui.kit.components.ToolbarComponent
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.profile.component.api.DeleteProfileComponent
import org.jetbrains.compose.resources.stringResource
import securitychat.common.icons_kit.generated.resources.ic_back
import securitychat.common.localization.generated.resources.delete_profile_description
import securitychat.common.localization.generated.resources.delete_profile_title

@Composable
internal fun DeleteProfileScreen(
    component: DeleteProfileComponent,
) {
    Screen(component) { state: DeleteProfileState, vm: DeleteProfileViewModel ->
        DeleteProfileScreenContent(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            state = state,
            onBackClicked = component::onBackClicked,
            onDeleteClicked = vm::onDeleteClicked,
            onDismissConfirmDialog = vm::onDismissConfirmDialog,
            onConfirmDeleteClicked = vm::onConfirmDeleteClicked,
        )
    }
}

@Composable
private fun DeleteProfileScreenContent(
    modifier: Modifier,
    state: DeleteProfileState,
    onBackClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onDismissConfirmDialog: () -> Unit,
    onConfirmDeleteClicked: () -> Unit,
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
                    text = stringResource(StringRes.delete_profile_title),
                ),
                endContent = null,
            )
            Column(
                modifier = Modifier
                    .widthIn(max = MAX_CONTENT_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = stringResource(StringRes.delete_profile_description),
                    color = AppTheme.colors.textPrimary,
                    style = AppTheme.typography.body,
                )
            }
        }
    }
}

@Preview
@Composable
internal fun DeleteProfileScreenPreview() {
    AppTheme {
        DeleteProfileScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = DeleteProfileState(
                showLoading = false,
            ),
            onBackClicked = {},
            onDeleteClicked = {},
            onDismissConfirmDialog = {},
            onConfirmDeleteClicked = {},
        )
    }
}

@Preview
@Composable
internal fun DeleteProfileScreenPreviewConfirmDialog() {
    AppTheme {
        DeleteProfileScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = DeleteProfileState(
                showLoading = false,
            ),
            onBackClicked = {},
            onDeleteClicked = {},
            onDismissConfirmDialog = {},
            onConfirmDeleteClicked = {},
        )
    }
}

@Preview
@Composable
internal fun DeleteProfileScreenPreviewLoading() {
    AppTheme {
        DeleteProfileScreenContent(
            modifier = Modifier.fillMaxSize(),
            state = DeleteProfileState(
                showLoading = true,
            ),
            onBackClicked = {},
            onDeleteClicked = {},
            onDismissConfirmDialog = {},
            onConfirmDeleteClicked = {},
        )
    }
}
