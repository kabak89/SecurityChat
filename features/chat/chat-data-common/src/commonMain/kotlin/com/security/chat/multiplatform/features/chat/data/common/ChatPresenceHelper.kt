package com.security.chat.multiplatform.features.chat.data.common

import com.security.chat.multiplatform.common.core.network.LiveEventsManager
import com.security.chat.multiplatform.features.chat.data.common.entity.OnlineInfoMessage
import com.security.chat.multiplatform.features.chat.data.common.entity.OnlineStatusPublisherMessage
import com.security.chat.multiplatform.features.chat.data.common.entity.OnlineStatusSubscribeMessage
import com.security.chat.multiplatform.features.chats.data.common.ChatsDataHelper
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

public interface ChatPresenceHelper {

    public suspend fun setUserOnline()

    public fun getOnlineParticipantIdsFlow(chatId: String): Flow<List<String>>
}

internal class ChatPresenceHelperImpl(
    private val liveEventsManager: LiveEventsManager,
    private val chatsDataHelper: ChatsDataHelper,
    private val userStorage: UserStorage,
) : ChatPresenceHelper {

    override suspend fun setUserOnline() {
        val userId = requireNotNull(userStorage.getUserId())
        liveEventsManager
            .subscribe<String, OnlineStatusPublisherMessage>(
                subscribeMessage = OnlineStatusPublisherMessage(userId = userId),
                type = "online_status_publish",
            )
            .collect {}
    }

    override fun getOnlineParticipantIdsFlow(chatId: String): Flow<List<String>> {
        return chatsDataHelper.getChatInfoFlow(chatId)
            .map { chat ->
                if (chat == null) return@map emptyList()
                val userId = requireNotNull(userStorage.getUserId())
                (chat.participantIds + chat.authorId).distinct().filter { it != userId }
            }
            .distinctUntilChanged()
            .flatMapLatest { participantIds ->
                if (participantIds.isEmpty()) return@flatMapLatest flowOf(emptyList())

                val participantFlows = participantIds.map { participantId ->
                    liveEventsManager
                        .subscribe<OnlineInfoMessage, OnlineStatusSubscribeMessage>(
                            subscribeMessage = OnlineStatusSubscribeMessage(
                                targetUserId = participantId,
                            ),
                            type = "online_status_receive",
                        )
                        .map { if (it.isOnline) participantId else null }
                        .onStart { emit(null) }
                }

                combine(participantFlows) { participants -> participants.filterNotNull() }
            }
            .distinctUntilChanged()
    }
}
