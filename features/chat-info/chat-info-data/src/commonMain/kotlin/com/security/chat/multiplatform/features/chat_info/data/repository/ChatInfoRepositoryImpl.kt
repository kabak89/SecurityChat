package com.security.chat.multiplatform.features.chat_info.data.repository

import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.features.add_chat.data.common.AddChatDataHelper
import com.security.chat.multiplatform.features.chat_info.data.entity.AddGroupChatParticipantsRequest
import com.security.chat.multiplatform.features.chat_info.data.entity.CreateGroupChatResponse
import com.security.chat.multiplatform.features.chat_info.data.mapper.toData
import com.security.chat.multiplatform.features.chat_info.domain.entity.ChatMember
import com.security.chat.multiplatform.features.chat_info.domain.repository.ChatInfoRepository
import com.security.chat.multiplatform.features.chats.data.common.ChatsDataHelper
import com.security.chat.multiplatform.features.users.data.common.UsersDataHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class ChatInfoRepositoryImpl(
    private val addChatDataHelper: AddChatDataHelper,
    private val chatsDataHelper: ChatsDataHelper,
    private val usersDataHelper: UsersDataHelper,
    private val networkManagerFactory: NetworkManagerFactory,
    private val networkConfig: NetworkConfig,
) : ChatInfoRepository {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(
            baseUrl = "${networkConfig.host}:${networkConfig.port}",
            needAuthorization = true,
        )
    }

    override suspend fun getCurrentMembersFlow(chatId: String): Flow<List<ChatMember>> {
        return chatsDataHelper.getChatInfo(chatId = chatId)
            .map { chatInfo ->
                if (chatInfo == null) return@map emptyList()
                val memberIds = chatInfo.participantIds + chatInfo.authorId
                memberIds.map { id ->
                    val userInfo = usersDataHelper.getOrFetchUser(id)
                    ChatMember(
                        id = userInfo.id,
                        username = userInfo.username,
                    )
                }
            }
    }

    override suspend fun searchMember(username: String): ChatMember {
        return addChatDataHelper
            .findUser(username)
            .let {
                ChatMember(
                    id = it.userId,
                    username = it.login,
                )
            }
    }

    override suspend fun fetchChatInfo(chatId: String) {
        chatsDataHelper.fetchChatInfo(chatId)
    }

    override suspend fun addMembers(
        chatId: String,
        memberIds: List<String>,
    ) {
        val result: CreateGroupChatResponse = networkManager.runPost(
            relativePath = "/chats/participants",
            request = AddGroupChatParticipantsRequest(
                chatId = chatId,
                participantIds = memberIds,
            ),
        )

        chatsDataHelper.saveChatInfo(result.toData())
    }
}
