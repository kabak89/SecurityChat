package com.security.chat.multiplatform.features.chat.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class MessagesPagingSource(
    private val chatId: String,
    private val chatStorage: ChatStorage,
    dispatcherProvider: DispatcherProviderInterface,
) : PagingSource<Long, MessageSM>() {

    private val scope = CoroutineScope(
        dispatcherProvider.IO + CoroutineExceptionHandler { _, e ->
            Log.e(e, "error in MessagesPagingSource(chatId=$chatId)")
        },
    )

    init {
        chatStorage.observeMessagesChanges(chatId = chatId)
            .drop(count = 1)
            .onEach { invalidate() }
            .launchIn(scope)
        registerInvalidatedCallback { scope.cancel() }
    }

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, MessageSM> {
        return try {
            val limit = params.loadSize.toLong()
            val items = when (params) {
                is LoadParams.Prepend -> {
                    chatStorage
                        .getNewerMessages(
                            chatId = chatId,
                            afterTimestamp = params.key,
                            limit = limit,
                        )
                }

                is LoadParams.Append -> {
                    chatStorage
                        .getOlderMessages(
                            chatId = chatId,
                            beforeTimestamp = params.key,
                            limit = limit,
                        )
                }

                is LoadParams.Refresh -> {
                    params.key?.let { key ->
                        chatStorage.getOlderMessages(
                            chatId = chatId,
                            beforeTimestamp = key,
                            limit = limit,
                        )
                    } ?: chatStorage.getNewestMessages(chatId = chatId, limit = limit)
                }
            }
            LoadResult.Page(
                data = items,
                prevKey = items.firstOrNull()?.timestamp,
                nextKey = items.lastOrNull()?.timestamp,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Long, MessageSM>): Long? {
        val anchor = state.anchorPosition ?: return null
        return state.closestItemToPosition(anchor)?.timestamp?.plus(1L)
    }
}
