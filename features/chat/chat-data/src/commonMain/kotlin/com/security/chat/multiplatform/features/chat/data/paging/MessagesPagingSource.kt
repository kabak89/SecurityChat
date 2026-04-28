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
                is LoadParams.Prepend -> loadPrepend(key = params.key, limit = limit)
                is LoadParams.Append -> loadAppend(key = params.key, limit = limit)
                is LoadParams.Refresh -> loadRefresh(anchorTimestamp = params.key, limit = limit)
            }
            LoadResult.Page(
                data = items,
                prevKey = items.firstOrNull()?.timestamp,
                nextKey = items.lastOrNull()?.timestamp,
            )
        } catch (e: Exception) {
            Log.e(e, "error in MessagesPagingSource")
            LoadResult.Error(e)
        }
    }

    private suspend fun loadPrepend(key: Long, limit: Long): List<MessageSM> {
        return chatStorage
            .getClosestNewerMessages(
                chatId = chatId,
                afterTimestamp = key,
                limit = limit,
            )
            .asReversed()
    }

    private suspend fun loadAppend(key: Long, limit: Long): List<MessageSM> {
        return chatStorage.getOlderMessages(
            chatId = chatId,
            beforeTimestamp = key,
            limit = limit,
        )
    }

    private suspend fun loadRefresh(anchorTimestamp: Long?, limit: Long): List<MessageSM> {
        if (anchorTimestamp == null) {
            return chatStorage.getNewestMessages(chatId = chatId, limit = limit)
        }
        val newerHalfLimit = limit / 2
        val newerAsc = chatStorage.getClosestNewerMessages(
            chatId = chatId,
            afterTimestamp = anchorTimestamp,
            limit = newerHalfLimit,
        )
        val olderLimit = limit - newerAsc.size
        val olderAndAnchor = chatStorage.getOlderMessages(
            chatId = chatId,
            beforeTimestamp = anchorTimestamp + 1L,
            limit = olderLimit,
        )
        return newerAsc.asReversed() + olderAndAnchor
    }

    override fun getRefreshKey(state: PagingState<Long, MessageSM>): Long? {
        val anchor = state.anchorPosition ?: return null
        if (anchor < state.config.prefetchDistance) return null
        return state.closestItemToPosition(anchor)?.timestamp
    }
}
