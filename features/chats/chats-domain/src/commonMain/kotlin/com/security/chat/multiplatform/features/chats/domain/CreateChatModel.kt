package com.security.chat.multiplatform.features.chats.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chats.domain.entity.AddChatsState
import com.security.chat.multiplatform.features.chats.domain.entity.CreateChatResult
import com.security.chat.multiplatform.features.chats.domain.entity.FindUserResult
import com.security.chat.multiplatform.features.chats.domain.repo.ChatsRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.kode.remo.Task0

public interface CreateChatModel : ScopedModel {
    public val createPersonalChat: Task0<CreateChatResult>
    public val createGroupChat: Task0<CreateChatResult>

    public fun setPersonalChatUsername(username: String)
    public fun setGroupChatUsername(username: String)
    public fun getAddChatStateFlow(): Flow<AddChatsState>
}

internal class CreateChatModelImpl(
    private val chatsRepo: ChatsRepo,
    dispatcherProvider: DispatcherProviderInterface,
) : CreateChatModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
    ) {

    private val stateFlow = MutableStateFlow(State())

    override val createPersonalChat: Task0<CreateChatResult> =
        task { ->
            val username = stateFlow.value.personalChatUsername
            val result = chatsRepo.findUser(username = username)

            return@task when (result) {
                is FindUserResult.UserFound -> {
                    val createChatResult = chatsRepo.createChat(secondUserId = result.userId)
                    stateFlow.update { it.copy(personalChatUsername = "") }
                    chatsRepo.fetchChatsList()
                    createChatResult
                }
            }
        }

    override val createGroupChat: Task0<CreateChatResult>
        get() = TODO("Not yet implemented")

    override fun setPersonalChatUsername(username: String) {
        stateFlow.update { it.copy(personalChatUsername = username) }
    }

    override fun setGroupChatUsername(username: String) {
        stateFlow.update { it.copy(groupChatUsername = username) }
    }

    override fun getAddChatStateFlow(): Flow<AddChatsState> {
        return stateFlow
            .map {
                AddChatsState(
                    personalChatUsername = it.personalChatUsername,
                    groupChatUsername = it.groupChatUsername,
                )
            }
            .distinctUntilChanged()
    }

    private data class State(
        val personalChatUsername: String = "",
        val groupChatUsername: String = "",
    )
}