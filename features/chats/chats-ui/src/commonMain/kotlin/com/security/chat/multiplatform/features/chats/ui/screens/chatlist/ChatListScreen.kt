package com.security.chat.multiplatform.features.chats.ui.screens.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.security.chat.multiplatform.common.core.ui.SingleEventEffect
import com.security.chat.multiplatform.common.ui.kit.theme.AppTheme
import com.security.chat.multiplatform.features.chats.component.api.ChatListComponent
import com.security.chat.multiplatform.features.chats.ui.screens.chatlist.entity.ChatItem
import com.security.chat.multiplatform.features.chats.ui.screens.chatlist.entity.Chats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import securitychat.common.icons_kit.generated.resources.Res
import securitychat.common.icons_kit.generated.resources.ic_add
import securitychat.common.icons_kit.generated.resources.ic_settings

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
            onAddClicked = onAddClicked,
            onSettingsClicked = onSettingsClicked,
        )
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            isRefreshing = state.chatListIsLoading,
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

@Composable
private fun ToolbarComponent(
    modifier: Modifier = Modifier,
    onAddClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(0.2f),
            horizontalArrangement = Arrangement.Start,
        ) {
            IconButton(
                modifier = Modifier
                    .size(48.dp),
                onClick = onSettingsClicked,
                content = {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_settings),
                        tint = AppTheme.colors.element,
                        contentDescription = null,
                    )
                },
            )
        }
        Text(
            modifier = Modifier
                .weight(0.6f),
            text = "Chats",
            color = AppTheme.colors.textPrimary,
            style = AppTheme.typography.title,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier
                .weight(0.2f),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(
                modifier = Modifier
                    .size(48.dp),
                onClick = onAddClicked,
                content = {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_add),
                        tint = AppTheme.colors.element,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

@Preview
@Composable
internal fun ChatListContentPreview() {
    AppTheme {
        ChatListContent(
            modifier = Modifier.fillMaxSize(),
            state = ChatListState(
                chatListIsLoading = false,
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
            ),
            events = emptyFlow(),
            onAddClicked = {},
            onSettingsClicked = {},
            onRefreshChatsTriggered = {},
            onChatClicked = {},
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
                chatListIsLoading = true,
                chats = Chats.EMPTY,
            ),
            events = emptyFlow(),
            onAddClicked = {},
            onSettingsClicked = {},
            onRefreshChatsTriggered = {},
            onChatClicked = {},
        )
    }
}