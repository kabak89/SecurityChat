package com.security.chat.multiplatform.features.chat.ui.screens.groupchat

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.components.SideContent
import com.security.chat.multiplatform.common.ui.kit.components.ToolbarComponent
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chat.component.api.GroupChatComponent
import com.security.chat.multiplatform.features.chat.domain.entity.PickedImage
import com.security.chat.multiplatform.features.chat.ui.screens.common.component.StickToNewestMessageEffect
import com.security.chat.multiplatform.features.chat.ui.screens.common.component.SyncComponent
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component.ImageMessageComponent
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component.IncomingMessageComponent
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component.OutgoingMessageComponent
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component.rememberPhotoPickerLauncher
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.MessageUM
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
import securitychat.common.icons_kit.generated.resources.ic_attach
import securitychat.common.icons_kit.generated.resources.ic_back
import securitychat.common.icons_kit.generated.resources.ic_send

@Composable
internal fun GroupChatScreen(
    component: GroupChatComponent,
) {
    Screen(component) { state: GroupChatState, vm: GroupChatViewModel ->
        val messages = vm.messages.collectAsLazyPagingItems()

        GroupChatContent(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            state = state,
            messages = messages,
            events = vm.viewEvent,
            onBackClicked = component::onExitClicked,
            onMessageEdited = vm::onMessageEdited,
            onPhotoPicked = vm::onPhotoPicked,
            onSendMessageClicked = vm::onSendMessageClicked,
            onSyncClicked = vm::onSyncClicked,
        )
    }
}

@Composable
private fun GroupChatContent(
    modifier: Modifier,
    state: GroupChatState,
    messages: LazyPagingItems<MessageUM>,
    events: Flow<GroupChatEvent>,
    onBackClicked: () -> Unit,
    onMessageEdited: (String) -> Unit,
    onPhotoPicked: (PickedImage) -> Unit,
    onSendMessageClicked: () -> Unit,
    onSyncClicked: () -> Unit,
) {
    val photoPickerLauncher = rememberPhotoPickerLauncher(
        onPhotoPicked = onPhotoPicked,
    )
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
        val lazyListState = rememberLazyListState()
        StickToNewestMessageEffect(
            lazyListState = lazyListState,
            messages = messages,
        )
        MessagesComponent(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState),
            state = state,
            toolbarHeight = with(localDensity) { toolbarHeight.toDp() },
            editMessageComponentHeight = with(localDensity) { editMessageComponentHeight.toDp() },
            messages = messages,
            lazyListState = lazyListState,
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
            onMessageEdited = onMessageEdited,
            onAttachClicked = photoPickerLauncher::launch,
            onSendMessageClicked = onSendMessageClicked,
        )
    }
}

@Composable
private fun Toolbar(
    modifier: Modifier = Modifier,
    state: GroupChatState,
    onBackClicked: () -> Unit,
    onSyncClicked: () -> Unit,
) {
    ToolbarComponent(
        modifier = modifier,
        startContent = SideContent.Button(
            icon = DrawableRes.ic_back,
            onClicked = onBackClicked,
        ),
        centerContent = null,
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
private fun MessagesComponent(
    modifier: Modifier = Modifier,
    state: GroupChatState,
    toolbarHeight: Dp,
    editMessageComponentHeight: Dp,
    messages: LazyPagingItems<MessageUM>,
    lazyListState: LazyListState,
) {
    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        reverseLayout = true,
        contentPadding = PaddingValues(top = toolbarHeight, bottom = editMessageComponentHeight),
        content = {
            items(
                count = messages.itemCount,
                key = messages.itemKey { it.id },
                contentType = messages.itemContentType { message ->
                    when (message) {
                        is MessageUM.Incoming.Image -> "incoming_image"
                        is MessageUM.Incoming.Text -> "incoming_text"
                        is MessageUM.Outgoing.Image -> "outgoing_image"
                        is MessageUM.Outgoing.Text -> "outgoing_text"
                    }
                },
            ) { index ->
                val message = messages[index] ?: return@items
                when (message) {
                    is MessageUM.Incoming.Text -> {
                        val previous = if (index + 1 < messages.itemCount) {
                            messages.peek(index + 1)
                        } else {
                            null
                        }
                        val showSenderName =
                            (previous as? MessageUM.Incoming.Text)?.senderName != message.senderName
                        IncomingMessageComponent(
                            modifier = Modifier.fillMaxWidth(),
                            message = message,
                            showSenderName = showSenderName,
                        )
                    }

                    is MessageUM.Incoming.Image -> ImageMessageComponent(
                        modifier = Modifier.fillMaxWidth(),
                        message = message,
                        isOutgoing = false,
                    )

                    is MessageUM.Outgoing.Text -> OutgoingMessageComponent(
                        modifier = Modifier.fillMaxWidth(),
                        message = message,
                    )

                    is MessageUM.Outgoing.Image -> ImageMessageComponent(
                        modifier = Modifier.fillMaxWidth(),
                        message = message,
                        isOutgoing = true,
                    )
                }
            }
        },
    )
}

@Composable
private fun EditMessageComponent(
    modifier: Modifier = Modifier,
    message: String,
    onMessageEdited: (String) -> Unit,
    onAttachClicked: () -> Unit,
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
        Row(
            modifier = Modifier
                .align(alignment = Alignment.CenterVertically),
        ) {
            IconButton(
                modifier = Modifier
                    .size(48.dp),
                onClick = onAttachClicked,
                content = {
                    Icon(
                        modifier = Modifier
                            .size(32.dp),
                        imageVector = vectorResource(Res.drawable.ic_attach),
                        tint = AppTheme.colors.element,
                        contentDescription = null,
                    )
                },
            )
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
        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Preview
@Composable
internal fun GroupChatScreenPreview() {
    val previewMessages = flowOf(
        PagingData.from(
            data = listOf(
                MessageUM.Outgoing.Text(
                    id = "1",
                    text = "some text",
                    datetimeText = "12:10",
                ),
                MessageUM.Incoming.Text(
                    id = "2",
                    text = "some text 2",
                    datetimeText = "12:10",
                    senderName = "John",
                ),
                MessageUM.Outgoing.Image(
                    id = "3",
                    text = "image",
                    datetimeText = "12:11",
                ),
                MessageUM.Incoming.Image(
                    id = "4",
                    text = "image",
                    datetimeText = "12:11",
                ),
            ),
        ),
    )
        .collectAsLazyPagingItems()

    AppTheme {
        GroupChatContent(
            modifier = Modifier.fillMaxSize(),
            state = GroupChatState(
                message = "",
                syncState = UiLceState.NotStarted,
            ),
            messages = previewMessages,
            events = emptyFlow(),
            onBackClicked = {},
            onMessageEdited = {},
            onPhotoPicked = {},
            onSendMessageClicked = {},
            onSyncClicked = {},
        )
    }
}
