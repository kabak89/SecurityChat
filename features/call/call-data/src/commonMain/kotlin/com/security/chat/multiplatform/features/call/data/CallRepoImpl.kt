package com.security.chat.multiplatform.features.call.data

import com.security.chat.multiplatform.common.core.network.LiveEventsManager
import com.security.chat.multiplatform.common.core.network.entity.SocketEvent
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.call.data.entity.FocusedParticipantsMessage
import com.security.chat.multiplatform.features.call.data.entity.FocusedSubscribeMessage
import com.security.chat.multiplatform.features.call.domain.entity.CallParticipant
import com.security.chat.multiplatform.features.call.domain.entity.CallPresence
import com.security.chat.multiplatform.features.call.domain.repo.CallRepo
import com.security.chat.multiplatform.features.call.domain.repo.CallSession
import com.security.chat.multiplatform.features.user.data.storage.UserStorage
import com.security.chat.multiplatform.features.users.data.common.UsersDataHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

internal class CallRepoImpl(
    private val liveEventsManager: LiveEventsManager,
    private val userStorage: UserStorage,
    private val usersDataHelper: UsersDataHelper,
    private val json: Json,
) : CallRepo {
    private val activeSession = MutableStateFlow<ObservedCallSession?>(null)

    override fun getPresenceFlow(chatId: String): Flow<CallPresence> =
        flow {
            val userId = requireNotNull(userStorage.getUserId())
            coroutineScope {
                val observer = launch {
                    while (true) {
                        val session = ObservedCallSession(
                            chatId = chatId,
                            session = CallSessionImpl(chatId, liveEventsManager, json),
                        )
                        activeSession.value = session
                        try {
                            session.observe()
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Throwable) {
                            Log.e(error, "Call room observation failed")
                        } finally {
                            activeSession.compareAndSet(session, null)
                        }
                        delay(500L)
                    }
                }
                try {
                    val room = activeSession.flatMapLatest { session ->
                        if (session == null) flowOf(null) else session.snapshot.mapLatest { snapshot ->
                            snapshot?.let { session.connectionId to it }
                        }
                    }
                    emitAll(
                        combine(
                            liveEventsManager.subscribeEvents<FocusedParticipantsMessage, FocusedSubscribeMessage>(
                                subscribeMessage = FocusedSubscribeMessage(chatId),
                                type = "focused",
                            ),
                            room,
                        ) { focused, state -> focused to state }
                            .mapLatest { (focused, state) ->
                                val event = focused as? SocketEvent.Message
                                if (event == null || event.value.chatId != chatId) {
                                    CallPresence(
                                        connectionId = null,
                                        isFocused = false,
                                        participants = emptyList(),
                                        isCallInProgress = false,
                                    )
                                } else {
                                    val snapshot =
                                        state?.takeIf { it.first == event.connectionId }?.second
                                    CallPresence(
                                        connectionId = event.connectionId.takeIf { snapshot != null },
                                        isFocused = userId in event.value.userIds,
                                        participants = event.value.userIds.distinct()
                                            .filter { it != userId }
                                            .map { id ->
                                                CallParticipant(
                                                    id = id,
                                                    name = usersDataHelper.getOrFetchUser(id).username,
                                                    isInCall = id in snapshot?.participantUserIds.orEmpty(),
                                                )
                                            },
                                        isCallInProgress = snapshot?.participantUserIds?.isNotEmpty() == true,
                                    )
                                }
                            }.distinctUntilChanged(),
                    )
                } finally {
                    observer.cancel()
                }
            }
        }

    override suspend fun withCallSession(chatId: String, block: suspend (CallSession) -> Unit) {
        val session = checkNotNull(activeSession.value) { "Call screen is not observing the room" }
        check(session.chatId == chatId) { "Call room changed" }
        session.use(block)
    }
}
