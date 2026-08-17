package com.security.chat.multiplatform.features.chat_info.ui.screens.addmember

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.components.ButtonContent
import com.security.chat.multiplatform.common.ui.kit.components.ButtonPrimary
import com.security.chat.multiplatform.common.ui.kit.components.CenterContent
import com.security.chat.multiplatform.common.ui.kit.components.SideContent
import com.security.chat.multiplatform.common.ui.kit.components.ToolbarComponent
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogComponent
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chat_info.component.api.AddMemberComponent
import com.security.chat.multiplatform.features.chat_info.ui.screens.addmember.entity.FoundMember
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import securitychat.common.icons_kit.generated.resources.ic_back
import securitychat.common.icons_kit.generated.resources.ic_remove
import securitychat.common.localization.generated.resources.add_member_add
import securitychat.common.localization.generated.resources.add_member_find
import securitychat.common.localization.generated.resources.add_member_textfield_placeholder
import securitychat.common.localization.generated.resources.add_member_title

@Composable
internal fun AddMemberScreen(
    component: AddMemberComponent,
) {
    Screen(
        component = component,
        screenName = "AddMemberScreen",
    ) { state: AddMemberState, vm: AddMemberViewModel ->
        AddMemberContent(
            state = state,
            onBackClicked = component::onBackClicked,
            onUsernameTextChanged = vm::onUsernameTextChanged,
            onFindClicked = vm::onFindClicked,
            onRemoveMemberClicked = vm::onRemoveMemberClicked,
            onAddClicked = vm::onAddClicked,
        )
    }
}

@Composable
private fun AddMemberContent(
    state: AddMemberState,
    onBackClicked: () -> Unit,
    onUsernameTextChanged: (username: String) -> Unit,
    onFindClicked: () -> Unit,
    onRemoveMemberClicked: (memberId: String) -> Unit,
    onAddClicked: () -> Unit,
) {
    val hazeState = rememberHazeState()
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.backgroundPrimary)
                .navigationBarsPadding()
                .imePadding()
                .hazeSource(hazeState),
        ) {
            ToolbarComponent(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                startContent = SideContent.Button(
                    icon = DrawableRes.ic_back,
                    onClicked = onBackClicked,
                ),
                centerContent = CenterContent.Title(
                    text = stringResource(StringRes.add_member_title),
                ),
            )
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                value = state.username,
                onValueChange = onUsernameTextChanged,
                placeholder = {
                    Text(
                        text = stringResource(StringRes.add_member_textfield_placeholder),
                        style = AppTheme.typography.body,
                        color = AppTheme.colors.textSuppressed,
                    )
                },
                enabled = !state.smthIsLoading,
                maxLines = 1,
                textStyle = AppTheme.typography.body,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = AppTheme.colors.accent,
                    unfocusedIndicatorColor = AppTheme.colors.element,
                ),
            )
            Spacer(Modifier.height(16.dp))
            ButtonPrimary(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClicked = onFindClicked,
                content = if (state.searchInProgress) {
                    ButtonContent.Loading
                } else {
                    ButtonContent.Text(
                        text = stringResource(StringRes.add_member_find),
                    )
                },
                enabled = state.searchEnabled,
            )
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = state.foundMembers,
                    key = { it.id },
                ) { member ->
                    FoundMemberComponent(
                        modifier = Modifier
                            .fillMaxWidth(),
                        member = member,
                        onRemoveClicked = {
                            onRemoveMemberClicked(member.id)
                        },
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(16.dp))
            ButtonPrimary(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClicked = onAddClicked,
                content = if (state.addingInProgress) {
                    ButtonContent.Loading
                } else {
                    ButtonContent.Text(
                        text = stringResource(StringRes.add_member_add),
                    )
                },
                enabled = state.addingIsEnabled,
            )
            Spacer(Modifier.height(16.dp))
        }

        if (state.alertDialogDescriptor != null) {
            AlertDialogComponent(
                content = state.alertDialogDescriptor.content,
                hazeState = hazeState,
                onDismissRequest = state.alertDialogDescriptor.dismissAction,
                onPositiveButtonClicked = state.alertDialogDescriptor.positiveAction,
                onNegativeButtonClicked = state.alertDialogDescriptor.negativeAction,
            )
        }
    }
}

@Composable
private fun FoundMemberComponent(
    modifier: Modifier = Modifier,
    member: FoundMember,
    onRemoveClicked: () -> Unit,
) {
    Row(
        modifier = modifier,
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
                .align(Alignment.CenterVertically),
            text = member.name,
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onRemoveClicked),
        ) {
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center),
                painter = painterResource(DrawableRes.ic_remove),
                contentDescription = null,
                tint = AppTheme.colors.element,
            )
        }
    }
}

@Preview
@Composable
internal fun AddMemberScreenPreview() {
    AppTheme {
        AddMemberContent(
            state = AddMemberState(
                username = "",
                searchInProgress = false,
                addingInProgress = false,
                foundMembers = listOf(
                    FoundMember(
                        id = "1",
                        name = "member_1",
                    ),
                ),
                alertDialogDescriptor = null,
            ),
            onBackClicked = {},
            onUsernameTextChanged = {},
            onFindClicked = {},
            onRemoveMemberClicked = {},
            onAddClicked = {},
        )
    }
}
