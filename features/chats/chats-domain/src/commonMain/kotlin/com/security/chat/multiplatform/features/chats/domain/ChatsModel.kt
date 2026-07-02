package com.security.chat.multiplatform.features.chats.domain

import com.security.chat.multiplatform.common.core.domain.BaseModel
import com.security.chat.multiplatform.common.core.domain.ScopedModel
import com.security.chat.multiplatform.common.core.threading.DispatcherProviderInterface
import com.security.chat.multiplatform.features.chats.domain.entity.Chat
import com.security.chat.multiplatform.features.chats.domain.repo.ChatsRepo
import kotlinx.coroutines.flow.Flow
import ru.kode.remo.Task0

public interface ChatsModel : ScopedModel {
    public val fetchChatsList: Task0<Unit>

    public fun getChatListFlow(): Flow<List<Chat>>
    public fun isConnectedToInternetFlow(): Flow<Boolean>
}

internal class ChatsModelImpl(
    private val chatsRepo: ChatsRepo,
    dispatcherProvider: DispatcherProviderInterface,
) : ChatsModel,
    BaseModel(
        dispatcher = dispatcherProvider.Default,
    ) {

    override val fetchChatsList: Task0<Unit> =
        task { ->
            chatsRepo.fetchChatsList()
        }

    override fun getChatListFlow(): Flow<List<Chat>> {
        return chatsRepo.getChatsListFlow()
    }

    override fun isConnectedToInternetFlow(): Flow<Boolean> {
        return chatsRepo.isConnectedToInternetFlow()
    }
}