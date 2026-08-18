package com.security.chat.multiplatform.features.chat_info.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chat_info.domain.entity.ChatInfo
import com.security.chat.multiplatform.features.chat_info.domain.repository.ChatInfoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.kode.remo.Task0

public interface ChatInfoModel : ScopedModel {

    public val fetchInfo: Task0<Unit>

    public fun setChatId(id: String)
    public fun getChatInfoFlow(): Flow<ChatInfo?>
}

internal class ChatInfoModelImpl(
    private val chatInfoRepository: ChatInfoRepository,
    dispatcherProvider: DispatcherProviderInterface,
) : ChatInfoModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
    ) {

    private val stateFlow = MutableStateFlow(State())

    override val fetchInfo: Task0<Unit> =
        task { ->
            val chatId = requireNotNull(stateFlow.value.chatId)
            val addingMembersAllowed = chatInfoRepository.isAddingMembersAllowed(chatId)
            stateFlow.update { it.copy(isAddingMembersAllowed = addingMembersAllowed) }
        }

    override fun setChatId(id: String) {
        check(stateFlow.value.chatId == null)
        stateFlow.update { it.copy(chatId = id) }
    }

    override fun getChatInfoFlow(): Flow<ChatInfo?> {
        return stateFlow
            .map { state ->
                ChatInfo(isAddingMembersAllowed = state.isAddingMembersAllowed)
            }
            .distinctUntilChanged()
    }

    private data class State(
        val chatId: String? = null,
        val isAddingMembersAllowed: Boolean = false,
    )
}
