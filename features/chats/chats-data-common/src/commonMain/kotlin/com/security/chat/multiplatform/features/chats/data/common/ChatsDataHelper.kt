package com.security.chat.multiplatform.features.chats.data.common

import com.security.chat.multiplatform.features.chats.data.common.entity.ChatInfo
import com.security.chat.multiplatform.features.chats.data.common.mapper.toSM
import com.security.chat.multiplatform.features.chats.data.network.ChatsNetworkManager
import com.security.chat.multiplatform.features.chats.data.storage.ChatsStorage
import com.security.chat.multiplatform.features.chats.data.storage.entity.ChatSM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

public interface ChatsDataHelper {

    public suspend fun fetchChatInfo(chatId: String)
    public fun getChatInfoFlow(chatId: String): Flow<ChatInfo?>
    public suspend fun getChatInfo(chatId: String): ChatInfo?
    public suspend fun saveChatInfo(chat: ChatInfo)
}

internal class ChatsDataHelperImpl(
    private val chatsStorage: ChatsStorage,
    private val chatsNetworkManager: ChatsNetworkManager,
) : ChatsDataHelper {

    override suspend fun fetchChatInfo(chatId: String) {
        val response = chatsNetworkManager.getChatInfo(chatId)

        chatsStorage.saveChat(
            chat = ChatSM.GroupChat(
                id = response.id,
                authorId = response.authorId,
                members = response.participantIds,
            ),
        )
    }

    override fun getChatInfoFlow(chatId: String): Flow<ChatInfo?> {
        return chatsStorage.getChatFlow(chatId)
            .map { chat ->
                when (chat) {
                    is ChatSM.GroupChat -> ChatInfo(
                        id = chat.id,
                        authorId = chat.authorId,
                        participantIds = chat.members,
                    )

                    null -> null
                }
            }
    }

    override suspend fun getChatInfo(chatId: String): ChatInfo? {
        return chatsStorage.getChat(chatId)
            ?.let { chat ->
                ChatInfo(
                    id = chat.id,
                    authorId = chat.authorId,
                    participantIds = chat.members,
                )
            }
    }

    override suspend fun saveChatInfo(chat: ChatInfo) {
        chatsStorage.saveChat(chat.toSM())
    }
}
