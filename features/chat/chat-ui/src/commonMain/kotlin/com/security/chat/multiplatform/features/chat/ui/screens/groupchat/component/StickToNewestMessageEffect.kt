package com.security.chat.multiplatform.features.chat.ui.screens.groupchat.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.paging.compose.LazyPagingItems
import com.security.chat.multiplatform.features.chat.ui.screens.groupchat.entity.ItemWithId

@Composable
internal fun StickToNewestMessageEffect(
    lazyListState: LazyListState,
    messages: LazyPagingItems<out ItemWithId>,
) {
    var stickToBottom by remember { mutableStateOf(true) }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.isScrollInProgress }
            .collect { scrolling ->
                if (!scrolling) {
                    stickToBottom = lazyListState.firstVisibleItemIndex == 0 &&
                            lazyListState.firstVisibleItemScrollOffset == 0
                }
            }
    }

    LaunchedEffect(lazyListState, messages) {
        snapshotFlow { messages.itemSnapshotList.items.firstOrNull()?.id }
            .collect { newestId ->
                if (newestId != null && stickToBottom) {
                    lazyListState.animateScrollToItem(0)
                }
            }
    }
}