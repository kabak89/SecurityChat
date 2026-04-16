package com.security.chat.multiplatform.features.chats.ui.screens.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.security.chat.multiplatform.common.core.localization.StringRes
import com.security.chat.multiplatform.common.core.ui.SingleEventEffect
import com.security.chat.multiplatform.common.core.ui.entity.UiLceState
import com.security.chat.multiplatform.common.core.ui.entity.isLoading
import com.security.chat.multiplatform.common.icons.kit.DrawableRes
import com.security.chat.multiplatform.common.ui.kit.components.CenterContent
import com.security.chat.multiplatform.common.ui.kit.components.SideContent
import com.security.chat.multiplatform.common.ui.kit.components.ToolbarComponent
import com.security.chat.multiplatform.common.ui.kit.components.alertdialog.AlertDialogComponent
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chats.component.api.ChatListComponent
import com.security.chat.multiplatform.features.chats.ui.screens.chatlist.entity.ChatItem
import com.security.chat.multiplatform.features.chats.ui.screens.chatlist.entity.Chats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import securitychat.common.icons_kit.generated.resources.ic_add
import securitychat.common.icons_kit.generated.resources.ic_settings
import securitychat.common.localization.generated.resources.chat_list_title

@Composable
public fun ChatListScreen(
    component: ChatListComponent,
) {
    try {
        if (component.getDiScope().closed) return
    } catch (e: Exception) {
        println(e)
        return
    }

    val vm: ChatListViewModel = koinViewModel(
        viewModelStoreOwner = component,
        scope = component.getDiScope(),
    )

    val state = vm.viewState.collectAsStateWithLifecycle().value

    ChatListContent(
        modifier = Modifier
            .fillMaxSize(),
        state = state,
        events = vm.viewEvent,
        onAddClicked = component::onAddClicked,
        onSettingsClicked = component::onSettingsClicked,
        onRefreshChatsTriggered = vm::onRefreshChatsTriggered,
        onChatClicked = component::onChatClicked,
        onCloseErrorDialogClicked = vm::onCloseErrorDialogClicked,
        onReloadChatsClicked = vm::onReloadChatsClicked,
    )
}

@Composable
private fun ChatListContent(
    modifier: Modifier = Modifier,
    state: ChatListState,
    events: Flow<ChatListEvent>,
    onAddClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onRefreshChatsTriggered: () -> Unit,
    onChatClicked: (chatId: String) -> Unit,
    onCloseErrorDialogClicked: () -> Unit,
    onReloadChatsClicked: () -> Unit,
) {
    SingleEventEffect(
        sideEffectFlow = events,
        collector = { event ->
            //TODO
        },
    )

    Column(
        modifier = modifier
            .background(AppTheme.colors.backgroundPrimary)
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        ToolbarComponent(
            modifier = Modifier
                .fillMaxWidth(),
            startContent = SideContent.Button(
                icon = DrawableRes.ic_settings,
                onClicked = onSettingsClicked,
            ),
            centerContent = CenterContent.Title(
                text = stringResource(StringRes.chat_list_title),
            ),
            endContent = SideContent.Button(
                icon = DrawableRes.ic_add,
                onClicked = onAddClicked,
            ),
        )
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            isRefreshing = state.loadingState.isLoading,
            onRefresh = onRefreshChatsTriggered,
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
            ) {
                state.chats.items.forEach { chat ->
                    item(key = chat.id) {
                        ChatComponent(
                            modifier = Modifier.fillMaxWidth(),
                            chat = chat,
                            onChatClicked = onChatClicked,
                        )
                    }
                }
            }
        }
    }
    val errorDialogContent = state.errorDialogContent
    if (errorDialogContent != null) {
        AlertDialogComponent(
            content = errorDialogContent,
            onDismissRequest = onCloseErrorDialogClicked,
            onPositiveButtonClicked = onReloadChatsClicked,
            onNegativeButtonClicked = onCloseErrorDialogClicked,
        )
    }
}

@Composable
private fun ChatComponent(
    modifier: Modifier = Modifier,
    chat: ChatItem,
    onChatClicked: (String) -> Unit,
) {
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(
                onClick = {
                    onChatClicked.invoke(chat.id)
                },
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier,
            text = chat.companionName,
            color = AppTheme.colors.textPrimary,
            style = AppTheme.typography.title2,
        )
    }
}

@Preview
@Composable
internal fun ChatListContentPreview() {
    AppTheme {
        ChatListContent(
            modifier = Modifier.fillMaxSize(),
            state = ChatListState(
                loadingState = UiLceState.Ready,
                chats = Chats(
                    items = listOf(
                        ChatItem(
                            id = "id-1",
                            companionName = "user_1",
                        ),
                        ChatItem(
                            id = "id-2",
                            companionName = "user_2",
                        ),
                    ),
                ),
                errorDialogContent = null,
            ),
            events = emptyFlow(),
            onAddClicked = {},
            onSettingsClicked = {},
            onRefreshChatsTriggered = {},
            onChatClicked = {},
            onCloseErrorDialogClicked = {},
            onReloadChatsClicked = {},
        )
    }
}

@Preview
@Composable
internal fun ChatListContentLoadingPreview() {
    AppTheme {
        ChatListContent(
            modifier = Modifier.fillMaxSize(),
            state = ChatListState(
                loadingState = UiLceState.Loading,
                chats = Chats.EMPTY,
                errorDialogContent = null,
            ),
            events = emptyFlow(),
            onAddClicked = {},
            onSettingsClicked = {},
            onRefreshChatsTriggered = {},
            onChatClicked = {},
            onCloseErrorDialogClicked = {},
            onReloadChatsClicked = {},
        )
    }
}