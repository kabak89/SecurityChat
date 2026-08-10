package com.security.chat.multiplatform.features.add_chat.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.add_chat.domain.entity.AddChatsState
import com.security.chat.multiplatform.features.add_chat.domain.entity.ChatMember
import com.security.chat.multiplatform.features.add_chat.domain.entity.CreateChatResult
import com.security.chat.multiplatform.features.add_chat.domain.entity.FindUserResult
import com.security.chat.multiplatform.features.add_chat.domain.entity.SameUserError
import com.security.chat.multiplatform.features.add_chat.domain.repo.AddChatRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.kode.remo.Task0

public interface CreateChatModel : ScopedModel {
    public val createPersonalChat: Task0<CreateChatResult.PersonalChatCreated>
    public val findUserForGroupChat: Task0<Unit>
    public val createGroupChat: Task0<CreateChatResult.GroupChatCreated>

    public fun setPersonalChatUsername(username: String)
    public fun setGroupChatUsername(username: String)
    public fun getAddChatStateFlow(): Flow<AddChatsState>
}

internal class CreateChatModelImpl(
    private val addChatRepo: AddChatRepo,
    dispatcherProvider: DispatcherProviderInterface,
) : CreateChatModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
    ) {

    private val stateFlow = MutableStateFlow(State())

    override val createPersonalChat: Task0<CreateChatResult.PersonalChatCreated> =
        task { ->
            val username = stateFlow.value.personalChatUsername.trim()
            val result = addChatRepo.findUser(username = username)
            val createChatResult = addChatRepo.createPersonalChat(secondUserId = result.userId)
            stateFlow.update { it.copy(personalChatUsername = "") }
            addChatRepo.refreshChatsList()
            createChatResult
        }

    override val findUserForGroupChat: Task0<Unit> =
        task { ->
            val username = stateFlow.value.groupChatUsername.trim()
            val findUserResult = addChatRepo.findUser(username = username)

            if (findUserResult.userId == addChatRepo.getUserId()) {
                throw SameUserError()
            }

            stateFlow.update {
                val groupChatMembers = (it.groupChatMembers + findUserResult).distinct()
                it.copy(
                    groupChatMembers = groupChatMembers,
                    groupChatUsername = "",
                )
            }
        }

    override val createGroupChat: Task0<CreateChatResult.GroupChatCreated> =
        task { ->
            val members = stateFlow.value.groupChatMembers.map { it.userId }
            val result = addChatRepo.createGroupChat(members)
            addChatRepo.refreshChatsList()
            result
        }

    override fun setPersonalChatUsername(username: String) {
        stateFlow.update { it.copy(personalChatUsername = username) }
    }

    override fun setGroupChatUsername(username: String) {
        stateFlow.update { it.copy(groupChatUsername = username) }
    }

    override fun getAddChatStateFlow(): Flow<AddChatsState> {
        return stateFlow
            .map {
                val chatMembers = it.groupChatMembers
                    .map { member ->
                        ChatMember(
                            id = member.userId,
                            username = member.login,
                        )
                    }

                AddChatsState(
                    personalChatUsername = it.personalChatUsername,
                    groupChatUsername = it.groupChatUsername,
                    chatMembers = chatMembers,
                )
            }
            .distinctUntilChanged()
    }

    private data class State(
        val personalChatUsername: String = "",
        val groupChatUsername: String = "",
        val groupChatMembers: List<FindUserResult> = emptyList(),
    )
}
