package com.security.chat.multiplatform.features.chat.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.chat.data.storage.ChatStorage
import com.security.chat.multiplatform.features.chat.data.storage.entity.MessageSM
import kotlinx.coroutines.CancellationException
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
    private val initialPosition: MessagePosition?,
) : PagingSource<MessagePosition, PagedMessage>() {

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

    override suspend fun load(
        params: LoadParams<MessagePosition>,
    ): LoadResult<MessagePosition, PagedMessage> {
        return try {
            val limit = params.loadSize.toLong()
            val messages = when (params) {
                is LoadParams.Prepend -> loadPrepend(key = params.key, limit = limit)
                is LoadParams.Append -> loadAppend(key = params.key, limit = limit)
                is LoadParams.Refresh -> loadRefresh(anchor = params.key, limit = limit)
            }
            val quoteIds = messages.mapNotNull { it.replyToMessageId }.toSet()
            val quotes = chatStorage.getMessagesByIds(chatId = chatId, messageIds = quoteIds)
                .associateBy { it.id }
            LoadResult.Page(
                data = messages.map { message ->
                    val quote = message.replyToMessageId?.let { id ->
                        quotes[id]?.takeUnless { it is MessageSM.Image && !it.isDownloaded }.also {
                            if (it == null) {
                                Log.e(
                                    "Missing quoted message: chatId=$chatId, " +
                                            "messageId=${message.id}, replyToMessageId=$id",
                                )
                            }
                        }
                    }
                    PagedMessage(message = message, quotedMessage = quote)
                },
                prevKey = when (params) {
                    is LoadParams.Refresh -> messages.firstOrNull()?.toPosition()
                        .takeIf { params.key != null }

                    is LoadParams.Prepend -> messages.firstOrNull()?.toPosition()
                        .takeIf { messages.size.toLong() == limit }

                    is LoadParams.Append -> messages.firstOrNull()?.toPosition()
                },
                nextKey = messages.lastOrNull()?.toPosition(),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(e, "error in MessagesPagingSource")
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(
        state: PagingState<MessagePosition, PagedMessage>,
    ): MessagePosition? {
        val anchor = state.anchorPosition ?: return initialPosition
        if (state.pages.firstOrNull()?.prevKey == null && anchor < state.config.prefetchDistance) {
            return null
        }
        return state.closestItemToPosition(anchor)?.message?.toPosition()
    }

    private suspend fun loadPrepend(key: MessagePosition, limit: Long): List<MessageSM> {
        return chatStorage.getClosestNewerMessages(
            chatId = chatId,
            afterTimestamp = key.timestamp,
            afterId = key.id,
            limit = limit,
        ).asReversed()
    }

    private suspend fun loadAppend(key: MessagePosition, limit: Long): List<MessageSM> {
        return chatStorage.getMessagesAtOrBefore(
            chatId = chatId,
            beforeTimestamp = key.timestamp,
            beforeId = key.id,
            limit = limit + 1,
        ).filterNot { it.id == key.id }.take(limit.toInt())
    }

    private suspend fun loadRefresh(anchor: MessagePosition?, limit: Long): List<MessageSM> {
        if (anchor == null) {
            return chatStorage.getNewestMessages(chatId = chatId, limit = limit)
        }
        val newer = loadPrepend(key = anchor, limit = limit / 2)
        val olderAndAnchor = chatStorage.getMessagesAtOrBefore(
            chatId = chatId,
            beforeTimestamp = anchor.timestamp,
            beforeId = anchor.id,
            limit = limit - newer.size,
        )
        return newer + olderAndAnchor
    }
}

internal data class MessagePosition(val timestamp: Long, val id: String)

internal data class PagedMessage(val message: MessageSM, val quotedMessage: MessageSM?)

internal fun MessageSM.toPosition(): MessagePosition {
    return MessagePosition(timestamp = timestamp, id = id)
}
