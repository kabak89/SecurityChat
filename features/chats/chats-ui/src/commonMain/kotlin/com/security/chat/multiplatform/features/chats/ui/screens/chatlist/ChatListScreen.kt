package com.security.chat.multiplatform.features.chats.ui.screens.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.security.chat.multiplatform.common.core.ui.SingleEventEffect
import com.security.chat.multiplatform.features.chats.component.ChatListComponent
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import securitychat.features.chats.chats_ui.generated.resources.Res
import securitychat.features.chats.chats_ui.generated.resources.ic_add

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
        onRefreshChatsTriggered = vm::onRefreshChatsTriggered,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatListContent(
    modifier: Modifier = Modifier,
    state: ChatListState,
    events: Flow<ChatListEvent>,
    onAddClicked: () -> Unit,
    onRefreshChatsTriggered: () -> Unit,
) {
    SingleEventEffect(
        sideEffectFlow = events,
        collector = { event ->
            //TODO
        },
    )

    Column(
        modifier = modifier
            .background(Color.White)
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        ToolbarComponent(
            modifier = Modifier
                .fillMaxWidth(),
            onAddClicked = onAddClicked,
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
                        Box(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Alignment.CenterStart),
                                text = chat.id,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarComponent(
    modifier: Modifier = Modifier,
    onAddClicked: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.weight(0.2f))
        Text(
            modifier = Modifier
                .weight(0.6f),
            text = "Chats",
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
                        tint = Color.Black,
                        contentDescription = null,
                    )
                },
            )

        }
    }
}