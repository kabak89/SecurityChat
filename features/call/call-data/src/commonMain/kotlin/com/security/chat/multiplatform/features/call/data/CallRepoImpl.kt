package com.security.chat.multiplatform.features.call.data

import com.security.chat.multiplatform.common.core.network.LiveEventsManager
import com.security.chat.multiplatform.features.call.data.entity.FocusedParticipantsMessage
import com.security.chat.multiplatform.features.call.data.entity.FocusedSubscribeMessage
import com.security.chat.multiplatform.features.call.domain.entity.CallParticipant
import com.security.chat.multiplatform.features.call.domain.repo.CallRepo
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import com.security.chat.multiplatform.features.users.data.common.UsersDataHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

internal class CallRepoImpl(
    private val liveEventsManager: LiveEventsManager,
    private val userStorage: UserStorage,
    private val usersDataHelper: UsersDataHelper,
) : CallRepo {

    override fun getFocusedParticipantsFlow(chatId: String): Flow<List<CallParticipant>> =
        flow {
            emit(emptyList())
            val userId = requireNotNull(userStorage.getUserId())
            val focusedParticipants = liveEventsManager
                .subscribe<FocusedParticipantsMessage, FocusedSubscribeMessage>(
                    subscribeMessage = FocusedSubscribeMessage(chatId = chatId),
                    type = "focused",
                )
                .filter { it.chatId == chatId }
                .map { message -> message.userIds.distinct().filter { it != userId } }
                .distinctUntilChanged()
                .mapLatest { participantIds -> participantIds.map { getParticipant(it) } }
            emitAll(focusedParticipants)
        }

    private suspend fun getParticipant(userId: String): CallParticipant {
        val name = usersDataHelper.getOrFetchUser(userId).username
        return CallParticipant(id = userId, name = name)
    }
}
