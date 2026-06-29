package com.security.chat.multiplatform.features.chats.ui.screens.addchat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.core.ui.SingleEventEffect
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.MAX_CONTENT_WIDTH_DP
import com.security.chat.multiplatform.common.ui.kit.components.ButtonContent
import com.security.chat.multiplatform.common.ui.kit.components.ButtonPrimary
import com.security.chat.multiplatform.common.ui.kit.components.CenterContent
import com.security.chat.multiplatform.common.ui.kit.components.SideContent
import com.security.chat.multiplatform.common.ui.kit.components.ToolbarComponent
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogComponent
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chats.component.api.AddChatComponent
import com.security.chat.multiplatform.features.chats.ui.screens.addchat.entity.ChatDescriptor
import com.security.chat.multiplatform.features.chats.ui.screens.addchat.entity.ChatType
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.stringResource
import securitychat.common.icons_kit.generated.resources.ic_back
import securitychat.common.localization.generated.resources.create_chat_find_button
import securitychat.common.localization.generated.resources.create_chat_title
import securitychat.common.localization.generated.resources.create_chat_type_group
import securitychat.common.localization.generated.resources.create_chat_type_personal
import securitychat.common.localization.generated.resources.create_chat_username_placeholder

@Composable
public fun AddChatScreen(
    component: AddChatComponent,
) {
    Screen(component) { state: AddChatState, vm: AddChatViewModel ->
        AddChatContent(
            modifier = Modifier
                .fillMaxSize(),
            state = state,
            events = vm.viewEvent,
            onBackClicked = component::onBackClicked,
            onPersonalChatUsernameChanged = vm::onPersonalChatUsernameChanged,
            onPersonalChatFindClicked = vm::onPersonalChatFindClicked,
            onChatCreated = component::onChatCreated,
            onTypeSelected = vm::onTypeSelected,
        )
    }
}

@Composable
private fun AddChatContent(
    modifier: Modifier = Modifier,
    state: AddChatState,
    events: Flow<AddChatEvent>,
    onBackClicked: () -> Unit,
    onPersonalChatUsernameChanged: (String) -> Unit,
    onPersonalChatFindClicked: () -> Unit,
    onChatCreated: (id: String) -> Unit,
    onTypeSelected: (ChatType) -> Unit,
) {
    SingleEventEffect(
        sideEffectFlow = events,
        collector = { event ->
            when (event) {
                is AddChatEvent.ChatCreated -> onChatCreated(event.id)
            }
        },
    )
    val hazeState = rememberHazeState()
    Box(
        modifier = modifier
            .background(AppTheme.colors.backgroundPrimary)
            .fillMaxSize()
            .hazeSource(state = hazeState),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
        ) {
            ToolbarComponent(
                modifier = Modifier
                    .fillMaxWidth(),
                startContent = SideContent.Button(
                    icon = DrawableRes.ic_back,
                    onClicked = onBackClicked,
                ),
                centerContent = CenterContent.Title(
                    text = stringResource(StringRes.create_chat_title),
                ),
                endContent = null,
            )
            Column(
                modifier = Modifier
                    .widthIn(max = MAX_CONTENT_WIDTH_DP.dp)
                    .fillMaxWidth()
                    .weight(1f)
                    .align(Alignment.CenterHorizontally),
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                ChatCreateComponent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    activeType = state.activeType,
                    personalChat = state.personalChat,
                    groupChat = state.groupChat,
                    onUsernameChanged = onPersonalChatUsernameChanged,
                    onFindClicked = onPersonalChatFindClicked,
                )
                Spacer(modifier = Modifier.height(16.dp))
                ChatTypeSelector(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    selectedType = state.activeType,
                    onTypeSelected = onTypeSelected,
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    if (state.dialogDescriptor != null) {
        AlertDialogComponent(
            hazeState = hazeState,
            content = state.dialogDescriptor.content,
            onDismissRequest = state.dialogDescriptor.dismissAction,
            onPositiveButtonClicked = state.dialogDescriptor.positiveAction,
            onNegativeButtonClicked = state.dialogDescriptor.negativeAction,
        )
    }
}

@Composable
private fun ChatCreateComponent(
    modifier: Modifier = Modifier,
    activeType: ChatType,
    personalChat: ChatDescriptor.Personal,
    groupChat: ChatDescriptor.Group,
    onUsernameChanged: (String) -> Unit,
    onFindClicked: () -> Unit,
) {
    AnimatedContent(
        modifier = modifier,
        targetState = activeType,
        label = "PagerLikeTransition",
        transitionSpec = {
            if (targetState == ChatType.Personal) {
                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
            } else {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            }
        },
    ) { chatType ->
        Box(
            modifier = Modifier,
        ) {
            if (chatType == ChatType.Personal) {
                PersonalChat(
                    modifier = Modifier,
                    state = personalChat,
                    onUsernameTextChanged = onUsernameChanged,
                    onFindClicked = onFindClicked,
                )
            } else {
                GroupChat()
            }
        }
    }
}

@Composable
private fun PersonalChat(
    modifier: Modifier = Modifier,
    state: ChatDescriptor.Personal,
    onUsernameTextChanged: (String) -> Unit,
    onFindClicked: () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            value = state.username,
            onValueChange = onUsernameTextChanged,
            placeholder = {
                Text(
                    text = stringResource(StringRes.create_chat_username_placeholder),
                    style = AppTheme.typography.body,
                    color = AppTheme.colors.textSuppressed,
                )
            },
            enabled = !state.isLoading,
            maxLines = 1,
            textStyle = AppTheme.typography.body,
        )
        Spacer(Modifier.weight(1f))
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(alignment = Alignment.CenterHorizontally),
            )
        } else {
            ButtonPrimary(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                content = ButtonContent.Text(
                    text = stringResource(StringRes.create_chat_find_button),
                ),
                onClicked = onFindClicked,
                enabled = state.isFindButtonEnabled,
            )
        }
    }
}

@Composable
private fun GroupChat() {
    Text(
        text = "GroupChat",
        style = AppTheme.typography.title2,
    )
}


@Composable
private fun ChatTypeSelector(
    modifier: Modifier = Modifier,
    selectedType: ChatType,
    onTypeSelected: (ChatType) -> Unit,
) {
    Row(modifier = modifier) {
        SelectorText(
            modifier = Modifier,
            text = stringResource(StringRes.create_chat_type_personal),
            selected = selectedType == ChatType.Personal,
            onClicked = { onTypeSelected(ChatType.Personal) },
        )
        Spacer(Modifier.width(16.dp))
        SelectorText(
            modifier = Modifier,
            text = stringResource(StringRes.create_chat_type_group),
            selected = selectedType == ChatType.Group,
            onClicked = { onTypeSelected(ChatType.Group) },
        )
    }
}

@Composable
private fun SelectorText(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    onClicked: () -> Unit,
) {
    val color = if (selected) {
        AppTheme.colors.textPrimary
    } else {
        AppTheme.colors.textSecondary
    }
    val background = if (selected) {
        AppTheme.colors.backgroundSecondary
    } else {
        Color.Transparent
    }
    Text(
        modifier = modifier
            .clip(AppTheme.shapes.roundedRectangle16)
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.clickable(onClick = onClicked)
                },
            )
            .background(
                color = background,
                shape = AppTheme.shapes.roundedRectangle16,
            )
            .padding(all = 8.dp)
            .padding(horizontal = 8.dp),
        text = text,
        style = AppTheme.typography.title2,
        color = color,
    )
}

@Preview
@Composable
internal fun AddChatPreviewPersonal() {
    AppTheme {
        AddChatContent(
            modifier = Modifier.fillMaxSize(),
            state = AddChatState(
                personalChat = ChatDescriptor.Personal(
                    username = "",
                    isLoading = false,
                ),
                groupChat = ChatDescriptor.Group(
                    username = "",
                    isLoading = false,
                    addedUsers = emptyList(),
                ),
                activeType = ChatType.Personal,
                dialogDescriptor = null,
            ),
            events = emptyFlow(),
            onBackClicked = {},
            onChatCreated = {},
            onPersonalChatUsernameChanged = {},
            onPersonalChatFindClicked = {},
            onTypeSelected = {},
        )
    }
}

@Preview
@Composable
internal fun AddChatPreviewGroup() {
    AppTheme {
        AddChatContent(
            modifier = Modifier.fillMaxSize(),
            state = AddChatState(
                personalChat = ChatDescriptor.Personal(
                    username = "",
                    isLoading = false,
                ),
                groupChat = ChatDescriptor.Group(
                    username = "",
                    isLoading = false,
                    addedUsers = emptyList(),
                ),
                activeType = ChatType.Group,
                dialogDescriptor = null,
            ),
            events = emptyFlow(),
            onBackClicked = {},
            onChatCreated = {},
            onPersonalChatUsernameChanged = {},
            onPersonalChatFindClicked = {},
            onTypeSelected = {},
        )
    }
}