package com.security.chat.multiplatform.features.users.data.network

import com.security.chat.multiplatform.common.core.network.NetworkManager
import com.security.chat.multiplatform.common.core.network.NetworkManagerFactory
import com.security.chat.multiplatform.features.users.data.network.entity.FindUserResponse
import com.security.chat.multiplatform.features.users.data.network.entity.UserNM
import com.security.chat.multiplatform.features.users.data.network.mapper.toNM

public interface UsersNetworkManager {
    public suspend fun fetchUser(id: String): UserNM
}

internal class UsersNetworkManagerImpl(
    private val networkManagerFactory: NetworkManagerFactory,
) : UsersNetworkManager {

    private val networkManager: NetworkManager by lazy {
        networkManagerFactory.build(baseUrl = "http://192.168.1.5:80")
    }

    override suspend fun fetchUser(id: String): UserNM {
        return networkManager.runGet<FindUserResponse>(
            relativePath = "/users/info",
            request = mapOf("id" to id),
        )
            .toNM()
    }
}