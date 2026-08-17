package com.security.chat.multiplatform.features.chats.data.common

import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.features.chats.data.common.entity.ChatInfo
import com.security.chat.multiplatform.features.chats.data.common.entity.network.ChatDetailsResponse
import com.security.chat.multiplatform.features.chats.data.common.mapper.toSM
import com.security.chat.multiplatform.features.chats.data.storage.ChatsStorage
import com.security.chat.multiplatform.features.chats.data.storage.entity.ChatSM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

public interface ChatsDataHelper {

    public suspend fun fetchChatInfo(chatId: String)
    public fun getChatInfo(chatId: String): Flow<ChatInfo?>
    public suspend fun saveChatInfo(chat: ChatInfo)
}

internal class ChatsDataHelperImpl(
    private val chatsStorage: ChatsStorage,
    private val networkManagerFactory: NetworkManagerFactory,
    private val networkConfig: NetworkConfig,
) : ChatsDataHelper {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(
            baseUrl = "${networkConfig.host}:${networkConfig.port}",
            needAuthorization = true,
        )
    }

    override suspend fun fetchChatInfo(chatId: String) {
        val response: ChatDetailsResponse = networkManager.runGet(
            relativePath = "/chats/info",
            request = mapOf("id" to chatId),
        )

        chatsStorage.saveChat(
            chat = ChatSM.GroupChat(
                id = response.id,
                authorId = response.authorId,
                members = response.participantIds,
            ),
        )
    }

    override fun getChatInfo(chatId: String): Flow<ChatInfo?> {
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

    override suspend fun saveChatInfo(chat: ChatInfo) {
        chatsStorage.saveChat(chat.toSM())
    }
}
