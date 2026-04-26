package com.security.chat.multiplatform.features.chat.ui.screens.personalchat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.security.chat.multiplatform.common.core.ui.Screen
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState
import com.security.chat.multiplatform.common.core.ui.entity.isLoading
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.components.ButtonContent
import com.security.chat.multiplatform.common.ui.kit.components.CenterContent
import com.security.chat.multiplatform.common.ui.kit.components.SideContent
import com.security.chat.multiplatform.common.ui.kit.components.ToolbarComponent
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chat.component.api.PersonalChatComponent
import com.security.chat.multiplatform.features.chat.ui.screens.personalchat.entity.InterlocutorUM
import com.security.chat.multiplatform.features.chat.ui.screens.personalchat.entity.MessageUM
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.resources.vectorResource
import securitychat.common.icons_kit.generated.resources.Res
import securitychat.common.icons_kit.generated.resources.ic_back
import securitychat.common.icons_kit.generated.resources.ic_send
import securitychat.common.icons_kit.generated.resources.ic_sync

@Composable
internal fun PersonalChatScreen(
    component: PersonalChatComponent,
) {
    Screen(component) { state: PersonalChatState, vm: PersonalChatViewModel ->
        val messages = vm.messages.collectAsLazyPagingItems()
        PersonalChatContent(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            state = state,
            messages = messages,
            events = vm.viewEvent,
            onBackClicked = component::onExitClicked,
            onMessageEdited = vm::onMessageEdited,
            onSendMessageClicked = vm::onSendMessageClicked,
            onSyncClicked = vm::onSyncClicked,
        )
    }
}

@Composable
private fun PersonalChatContent(
    modifier: Modifier,
    state: PersonalChatState,
    messages: LazyPagingItems<MessageUM>,
    events: Flow<PersonalChatEvent>,
    onBackClicked: () -> Unit,
    onMessageEdited: (String) -> Unit,
    onSendMessageClicked: () -> Unit,
    onSyncClicked: () -> Unit,
) {
    val hazeState = rememberHazeState()
    val backgroundPrimaryColor = AppTheme.colors.backgroundPrimary
    val hazeStyle = remember {
        HazeStyle(
            backgroundColor = backgroundPrimaryColor,
            tint = HazeTint(
                color = backgroundPrimaryColor.copy(alpha = 0.5f),
            ),
            blurRadius = 16.dp,
            fallbackTint = HazeTint(
                color = backgroundPrimaryColor.copy(alpha = 0.95f),
            ),
        )
    }
    Box(
        modifier = modifier
            .background(AppTheme.colors.backgroundPrimary)
            .fillMaxSize(),
    ) {
        var toolbarHeight by remember { mutableStateOf(0) }
        var editMessageComponentHeight by remember { mutableStateOf(0) }
        val localDensity = LocalDensity.current
        MessagesComponent(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState),
            state = state,
            toolbarHeight = with(localDensity) { toolbarHeight.toDp() },
            editMessageComponentHeight = with(localDensity) { editMessageComponentHeight.toDp() },
            messages = messages,
        )
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Toolbar(
            modifier = Modifier
                .fillMaxWidth()
                .hazeEffect(
                    state = hazeState,
                    style = hazeStyle,
                )
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .onGloballyPositioned {
                    val topPaddingInPixels = with(localDensity) {
                        statusBarHeight.toPx()
                    }.toInt()
                    toolbarHeight = it.size.height + topPaddingInPixels
                },
            state = state,
            onBackClicked = onBackClicked,
            onSyncClicked = onSyncClicked,
        )
        val isImeVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() != 0.dp
        val editPanelBottomPadding = if (isImeVisible) {
            0.dp
        } else {
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        }
        val animatedPadding by animateDpAsState(
            targetValue = editPanelBottomPadding,
            label = "editPanelBottomPadding",
        )
        EditMessageComponent(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .hazeEffect(
                    state = hazeState,
                    style = hazeStyle,
                )
                .background(AppTheme.colors.backgroundSecondary.copy(alpha = 0.5f))
                .padding(bottom = animatedPadding)
                .onGloballyPositioned {
                    val bottomPaddingInPixels = with(localDensity) {
                        editPanelBottomPadding.toPx()
                    }.toInt()
                    editMessageComponentHeight = it.size.height + bottomPaddingInPixels
                },
            message = state.message,
            sendingInProgress = state.sendingMessageInProgress,
            onMessageEdited = onMessageEdited,
            onSendMessageClicked = onSendMessageClicked,
        )
    }
}

@Composable
private fun MessagesComponent(
    modifier: Modifier = Modifier,
    state: PersonalChatState,
    toolbarHeight: Dp,
    editMessageComponentHeight: Dp,
    messages: LazyPagingItems<MessageUM>,
) {
    LazyColumn(
        modifier = modifier,
        state = rememberLazyListState(),
        reverseLayout = true,
        contentPadding = PaddingValues(top = toolbarHeight, bottom = editMessageComponentHeight),
        content = {
            items(
                count = messages.itemCount,
                key = messages.itemKey { it.id },
                contentType = messages.itemContentType { message ->
                    when (message) {
                        is MessageUM.Incoming -> "incoming"
                        is MessageUM.Outgoing -> "outgoing"
                    }
                },
            ) { index ->
                val message = messages[index] ?: return@items
                when (message) {
                    is MessageUM.Incoming -> IncomingMessageComponent(
                        modifier = Modifier.fillMaxWidth(),
                        message = message,
                    )

                    is MessageUM.Outgoing -> OutgoingMessageComponent(
                        modifier = Modifier.fillMaxWidth(),
                        message = message,
                    )
                }
            }
        },
    )
}

@Composable
private fun Toolbar(
    modifier: Modifier = Modifier,
    state: PersonalChatState,
    onBackClicked: () -> Unit,
    onSyncClicked: () -> Unit,
) {
    ToolbarComponent(
        modifier = modifier,
        startContent = SideContent.Button(
            icon = DrawableRes.ic_back,
            onClicked = onBackClicked,
        ),
        centerContent = CenterContent.Title(
            text = state.interlocutor?.name.toString(),
        ),
        endContent = SideContent.Custom(
            content = {
                SyncComponent(
                    syncState = state.syncState,
                    onSyncClicked = onSyncClicked,
                )
            },
        ),
    )
}

@Composable
private fun OutgoingMessageComponent(
    modifier: Modifier = Modifier,
    message: MessageUM.Outgoing,
) {
    Row(
        modifier = modifier
            .padding(all = 16.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Text(
            modifier = Modifier
                .padding(start = 40.dp),
            text = message.text,
            color = AppTheme.colors.textPrimary,
            style = AppTheme.typography.body,
        )
    }
}

@Composable
private fun IncomingMessageComponent(
    modifier: Modifier = Modifier,
    message: MessageUM.Incoming,
) {
    Row(
        modifier = modifier
            .padding(all = 16.dp),
    ) {
        Text(
            modifier = Modifier
                .padding(end = 40.dp),
            text = message.text,
            color = AppTheme.colors.textPrimary,
            style = AppTheme.typography.body,
        )
    }
}

@Composable
private fun SyncComponent(
    modifier: Modifier = Modifier,
    syncState: UiLceState,
    onSyncClicked: () -> Unit,
) {
    val inProgress = remember(syncState) {
        syncState.isLoading
    }

    val rotation = if (inProgress) {
        val transition = rememberInfiniteTransition(label = "sync-rotation")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = -360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800),
                repeatMode = RepeatMode.Restart,
            ),
            label = "sync-rotation-angle",
        ).value
    } else {
        0f
    }

    Box(
        modifier = modifier.rotate(rotation),
    ) {
        ButtonContent(
            icon = DrawableRes.ic_sync,
            onClicked = onSyncClicked,
        )
    }
}

@Composable
private fun EditMessageComponent(
    modifier: Modifier = Modifier,
    message: String,
    sendingInProgress: Boolean,
    onMessageEdited: (String) -> Unit,
    onSendMessageClicked: () -> Unit,
) {
    Row(
        modifier = modifier,
    ) {
        val textSecondaryColor = AppTheme.colors.textSecondary
        val placeholderColor = remember { textSecondaryColor.copy(alpha = 0.3f) }
        TextField(
            modifier = Modifier
                .weight(1f),
            value = message,
            maxLines = 3,
            onValueChange = onMessageEdited,
            placeholder = {
                Text("Message")
            },
            colors = TextFieldDefaults.colors().copy(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = AppTheme.colors.textSecondary,
                unfocusedTextColor = AppTheme.colors.textSecondary,
                cursorColor = AppTheme.colors.element,
                focusedPlaceholderColor = placeholderColor,
                unfocusedPlaceholderColor = placeholderColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            shape = RectangleShape,
            textStyle = AppTheme.typography.body,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .align(alignment = Alignment.CenterVertically),
        ) {
            if (sendingInProgress) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp),
                    color = AppTheme.colors.element,
                )
            } else {
                IconButton(
                    modifier = Modifier
                        .size(48.dp),
                    onClick = onSendMessageClicked,
                    content = {
                        Icon(
                            modifier = Modifier
                                .size(32.dp),
                            imageVector = vectorResource(Res.drawable.ic_send),
                            tint = AppTheme.colors.element,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Preview
@Composable
internal fun PersonalChatScreenPreview() {
    val previewMessages = flowOf(
        PagingData.from(
            data = listOf(
                MessageUM.Outgoing(
                    id = "1",
                    text = "some text",
                ),
                MessageUM.Incoming(
                    id = "2",
                    text = "some text 2",
                ),
            ),
        ),
    ).collectAsLazyPagingItems()

    AppTheme {
        PersonalChatContent(
            modifier = Modifier.fillMaxSize(),
            state = PersonalChatState(
                message = "",
                sendingMessageInProgress = false,
                syncState = UiLceState.Ready,
                interlocutor = InterlocutorUM(
                    name = "User 1",
                    isOnline = false,
                ),
            ),
            messages = previewMessages,
            events = emptyFlow(),
            onBackClicked = {},
            onMessageEdited = {},
            onSendMessageClicked = {},
            onSyncClicked = {},
        )
    }
}
