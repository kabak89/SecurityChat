package com.security.chat.multiplatform.features.chats.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chats.domain.entity.AddChatsState
import com.security.chat.multiplatform.features.chats.domain.entity.ChatDescription
import com.security.chat.multiplatform.features.chats.domain.entity.CreateChatResult
import com.security.chat.multiplatform.features.chats.domain.entity.FindUserResult
import com.security.chat.multiplatform.features.chats.domain.repo.ChatsRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.kode.remo.Task0

public interface ChatsModel : ScopedModel {

    public val createChat: Task0<CreateChatResult>
    public val fetchChatsList: Task0<Unit>

    public fun setUsername(username: String)
    public fun getAddChatStateFlow(): Flow<AddChatsState>
    public fun getChatListFlow(): Flow<List<ChatDescription>>

}

internal class ChatsModelImpl(
    private val chatsRepo: ChatsRepo,
    coroutineScope: CoroutineScope,
    dispatcherProvider: DispatcherProviderInterface,
) : ChatsModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
        coroutineScope = coroutineScope,
    ) {

    private val stateFlow = MutableStateFlow(State())

    override val createChat: Task0<CreateChatResult> =
        task { ->
            val username = stateFlow.value.username
            val result = chatsRepo.findUser(username = username)

            return@task when (result) {
                is FindUserResult.NotFound -> {
                    CreateChatResult.UserNotFound
                }

                is FindUserResult.UserFound -> {
                    val createChatResult = chatsRepo.createChat(secondUserId = result.userId)
                    stateFlow.update { it.copy(username = "") }
                    fetchChatsList.start()
                    createChatResult
                }
            }
        }

    override val fetchChatsList: Task0<Unit> =
        task { ->
            val chats = chatsRepo.getChatsList()
            stateFlow.update { it.copy(chatList = chats) }
        }

    override fun setUsername(username: String) {
        stateFlow.update { it.copy(username = username) }
    }

    override fun getAddChatStateFlow(): Flow<AddChatsState> {
        return stateFlow
            .map { state ->
                AddChatsState(
                    username = state.username,
                )
            }
            .distinctUntilChanged()
    }

    override fun getChatListFlow(): Flow<List<ChatDescription>> {
        return stateFlow
            .map { it.chatList }
            .distinctUntilChanged()
    }

    private data class State(
        val username: String = "",
        val chatList: List<ChatDescription> = emptyList(),
    )

}