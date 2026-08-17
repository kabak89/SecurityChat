package com.security.chat.multiplatform.features.chat_info.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.common.log.Log
import com.security.chat.multiplatform.features.chat_info.domain.entity.ChatMember
import com.security.chat.multiplatform.features.chat_info.domain.entity.StateInfo
import com.security.chat.multiplatform.features.chat_info.domain.entity.errors.UserAlreadyInChat
import com.security.chat.multiplatform.features.chat_info.domain.entity.errors.UserAlreadyInListForAdding
import com.security.chat.multiplatform.features.chat_info.domain.repository.ChatInfoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import ru.kode.remo.Task0

public interface AddMemberModel : ScopedModel {

    public val search: Task0<Unit>
    public val fetchChatInfo: Task0<Unit>
    public val addMembers: Task0<Unit>

    public fun setChatId(id: String)
    public fun setMemberName(username: String)
    public fun getStateFlow(): Flow<StateInfo>
    public fun removeMember(memberId: String)
}

internal class AddMemberModelImpl(
    private val chatInfoRepository: ChatInfoRepository,
    dispatcherProvider: DispatcherProviderInterface,
) : AddMemberModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
    ) {

    private val stateFlow = MutableStateFlow(State())

    override fun onPostStart() {
        super.onPostStart()

        stateFlow
            .mapNotNull { it.chatId }
            .take(1)
            .flatMapLatest { chatId ->
                chatInfoRepository.getCurrentMembersFlow(chatId)
            }
            .onEach { currentMembers ->
                stateFlow.update { it.copy(currentMembers = currentMembers) }
            }
            .launchIn(scope)
    }

    override fun setChatId(id: String) {
        check(stateFlow.value.chatId == null)
        stateFlow.update { it.copy(chatId = id) }
    }

    override val search: Task0<Unit> =
        task { ->
            val memberNameForSearch = stateFlow.value.memberNameForSearch.trim()

            if (memberNameForSearch.isBlank()) {
                Log.e("search query is empty")
                return@task
            }

            val foundMembers = stateFlow.value.foundMembers

            if (foundMembers.find { it.username == memberNameForSearch } != null) {
                throw UserAlreadyInListForAdding()
            }

            val currentMembers = stateFlow.value.currentMembers

            if (currentMembers.find { it.username == memberNameForSearch } != null) {
                throw UserAlreadyInChat()
            }

            val foundMember = chatInfoRepository.searchMember(memberNameForSearch)
            val newMembers = stateFlow.value.foundMembers + foundMember
            stateFlow.update {
                it.copy(
                    foundMembers = newMembers,
                    memberNameForSearch = "",
                )
            }
        }

    override val fetchChatInfo: Task0<Unit> =
        task { ->
            val chatId = requireNotNull(stateFlow.value.chatId)
            chatInfoRepository.fetchChatInfo(chatId)
        }

    override val addMembers: Task0<Unit> =
        task { ->
            val chatId = requireNotNull(stateFlow.value.chatId)
            val memberIds = stateFlow.value.foundMembers.map { it.id }

            if (memberIds.isEmpty()) {
                error("memberIds is empty")
            }

            chatInfoRepository.addMembers(
                chatId = chatId,
                memberIds = memberIds,
            )
        }

    override fun setMemberName(username: String) {
        stateFlow.update { it.copy(memberNameForSearch = username) }
    }

    override fun getStateFlow(): Flow<StateInfo> {
        return stateFlow
            .map { state ->
                StateInfo(
                    memberNameForSearch = state.memberNameForSearch,
                    foundMembers = state.foundMembers,
                )
            }
            .distinctUntilChanged()
    }

    override fun removeMember(memberId: String) {
        val foundMembers = stateFlow.value.foundMembers
        val memberToDelete = foundMembers.find { it.id == memberId }

        if (memberToDelete == null) {
            Log.e("member with id $memberId not found")
            return
        }

        val newList = foundMembers - memberToDelete
        stateFlow.update { it.copy(foundMembers = newList) }
    }

    private data class State(
        val chatId: String? = null,
        val memberNameForSearch: String = "",
        val foundMembers: List<ChatMember> = emptyList(),
        val currentMembers: List<ChatMember> = emptyList(),
    )
}
