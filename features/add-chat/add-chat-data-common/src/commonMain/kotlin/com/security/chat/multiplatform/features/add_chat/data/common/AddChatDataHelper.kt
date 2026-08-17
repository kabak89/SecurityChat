package com.security.chat.multiplatform.features.add_chat.data.common

import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.common.core.network.entity.NetworkConfig
import com.security.chat.multiplatform.features.add_chat.data.common.entity.FindUserResponse
import com.security.chat.multiplatform.features.add_chat.data.common.entity.SearchResult

public interface AddChatDataHelper {

    public suspend fun findUser(
        username: String,
    ): SearchResult
}

internal class AddChatDataHelperImpl(
    private val networkManagerFactory: NetworkManagerFactory,
    private val networkConfig: NetworkConfig,
) : AddChatDataHelper {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(
            baseUrl = "${networkConfig.host}:${networkConfig.port}",
            needAuthorization = true,
        )
    }

    override suspend fun findUser(
        username: String,
    ): SearchResult {
        val response: FindUserResponse = networkManager.runGet(
            relativePath = "/users/find",
            request = mapOf(
                "login" to username,
            ),
        )

        return SearchResult(
            userId = response.userId,
            login = response.login,
        )
    }
}
