package com.security.chat.multiplatform.features.chats.data.network

import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.features.chats.data.network.entity.ChatInfoNM

public interface ChatsNetworkManager {
    public suspend fun getChatInfo(chatId: String): ChatInfoNM
}

internal class ChatsNetworkManagerImpl(
    private val networkManagerFactory: NetworkManagerFactory,
    private val networkConfig: NetworkConfig,
) : ChatsNetworkManager {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(
            baseUrl = "${networkConfig.host}:${networkConfig.port}",
            needAuthorization = true,
        )
    }

    override suspend fun getChatInfo(chatId: String): ChatInfoNM =
        networkManager.runGet(
            relativePath = "/chats/info",
            request = mapOf("id" to chatId),
        )
}
